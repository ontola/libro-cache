package io.ontola.cache

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.DefaultHeaders
import io.ktor.features.ForwardedHeaderSupport
import io.ktor.features.StatusPages
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.features.deflate
import io.ktor.features.gzip
import io.ktor.features.maxAge
import io.ktor.features.minimumSize
import io.ktor.features.toLogString
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.ParametersBuilder
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.ontola.cache.assets.Assets
import io.ontola.cache.health.mountHealth
import io.ontola.cache.plugins.CSP
import io.ontola.cache.plugins.CacheConfig
import io.ontola.cache.plugins.CacheConfiguration
import io.ontola.cache.plugins.CacheSession
import io.ontola.cache.plugins.DataProxy
import io.ontola.cache.plugins.DeviceId
import io.ontola.cache.plugins.Logging
import io.ontola.cache.plugins.RedisAdapter
import io.ontola.cache.plugins.ServiceRegistry
import io.ontola.cache.plugins.Storage
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.plugins.requestTimings
import io.ontola.cache.routes.mountBulk
import io.ontola.cache.routes.mountIndex
import io.ontola.cache.routes.mountManifest
import io.ontola.cache.routes.mountMaps
import io.ontola.cache.routes.mountStatic
import io.ontola.cache.sessions.RedisSessionStorage
import io.ontola.cache.sessions.SessionData
import io.ontola.cache.sessions.signedTransformer
import io.ontola.cache.tenantization.Tenantization
import io.ontola.cache.util.configureCallLogging
import io.ontola.cache.util.isHtmlAccept
import kotlin.collections.set
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

fun ApplicationRequest.isHTML(): Boolean {
    val accept = headers["accept"] ?: ""
    val contentType = headers["content-type"] ?: ""

    return accept.isHtmlAccept() || contentType.contains("text/html")
}

@OptIn(KtorExperimentalLocationsAPI::class, ExperimentalLettuceCoroutinesApi::class, ExperimentalTime::class)
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(
    testing: Boolean = false,
    storage: StorageAdapter<String, String>? = null,
    client: HttpClient = createClient(),
) {
    val config = CacheConfig.fromEnvironment(environment.config, testing, client)
    val adapter = storage ?: RedisAdapter(RedisClient.create(config.redis.uri).connect().coroutines())

    install(Logging)

    install(StatusPages) {
        exception<TenantNotFoundException> {
            call.respond(HttpStatusCode.NotFound)
        }
        exception<BadGatewayException> {
            call.respond(HttpStatusCode.BadGateway)
        }
        exception<AuthenticationException> {
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> {
            call.respond(HttpStatusCode.Forbidden)
        }
        exception<Exception> { cause ->
            config.notify(cause)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    install(CacheConfiguration) {
        this.config = config
    }

    install(Assets)

    install(ServiceRegistry) {
        if (testing) {
            initFromTest(config.services)
        } else {
            initFrom(config.services)
        }
    }

    install(Storage) {
        this.adapter = adapter
        this.expiration = config.cacheExpiration
    }

    install(CSP)

    install(Locations)

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024)
        }
    }

    install(CallLogging) {
        configureCallLogging()
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            when (val status = call.response.status()) {
                HttpStatusCode.Found -> "$status: ${call.request.toLogString()} -> ${call.response.headers[HttpHeaders.Location]}"
                else -> {
                    val timings = call.requestTimings.joinToString { (name, time) -> "$name: ${time}ms" }
                    "$status - ${call.request.toLogString()} - timings: ($timings)"
                }
            }
        }
    }

    install(DefaultHeaders) {
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        header("X-Content-Type-Options", "nosniff")
        header("X-Engine", "Ktor")
        header("X-Frame-Options", "DENY")
        header("X-Powered-By", "Ontola")
        header("X-XSS-Protection", "1; mode=block")
    }

    install(ForwardedHeaderSupport)
    install(XForwardedHeaderSupport)

    install(Tenantization) {
        blacklist = listOf(
            "/favicon.ico",
            "/link-lib/cache/clear",
            "/link-lib/cache/status",
            "/d/health",
            "/metrics",
            "/static/",
            "/assets/",
            "/f_assets/",
            "/__webpack_hmr"
        )
    }

    install(Sessions) {
        cookie<SessionData>(
            name = "identity",
            storage = RedisSessionStorage(adapter),
        ) {
            cookie.httpOnly = true
            cookie.secure = true
        }
        cookie<String>("deviceId") {
            cookie.httpOnly = true
            cookie.maxAge = 365.days
            transform(
                signedTransformer(signingSecret = config.sessions.sessionSecret)
            )
        }
    }

    install(DeviceId)

    install(CacheSession) {
        legacyStorageAdapter = adapter
        val jwtToken = Algorithm.HMAC512(config.sessions.jwtEncryptionToken)
        jwtValidator = JWT.require(jwtToken)
            .withClaim("application_id", config.sessions.clientId)
            .build()
    }

    install(DataProxy) {
        val loginQuery = ParametersBuilder().apply {
            append("client_id", config.sessions.clientId)
            append("client_secret", config.sessions.clientSecret)
            append("grant_type", "password")
            append("scope", "user")
        }.build()
        val loginTransform = { req: ApplicationRequest ->
            val websiteIri = req.header("website-iri")

            Url("$websiteIri/oauth/token")
                .copy(parameters = loginQuery)
                .fullPath
        }

        binaryPaths = listOf(
            "/media_objects/",
        )
        excludedPaths = listOf(
            "/link-lib/bulk",
            "/d/health",
            "static",
        )
        contentTypes = listOf(
            ContentType.parse("application/hex+x-ndjson"),
            ContentType.parse("application/json"),
            ContentType.parse("application/ld+json"),
            ContentType.parse("application/n-quads"),
            ContentType.parse("application/n-triples"),
            ContentType.parse("text/turtle"),
            ContentType.parse("text/n3"),
        )
        extensions = listOf(
            "hndjson",
            "json",
            "jsonld",
            "nq",
            "nt",
            "ttl",
            "n3",
            "png",
            "rdf",
            "csv",
            "pdf]",
        )
        transforms[Regex("^/login$")] = loginTransform
        transforms[Regex("^/[\\w/]*/login$")] = loginTransform
    }

    routing {
        mountStatic()
        mountHealth()
        mountManifest()
        mountBulk()
        mountLogout()
        mountMaps()
        mountIndex(client)
    }
}
