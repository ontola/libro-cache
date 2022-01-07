package io.ontola.cache

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.fullPath
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.html.respondHtml
import io.ktor.server.locations.Locations
import io.ktor.server.logging.toLogString
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.CallLogging
import io.ktor.server.plugins.Compression
import io.ktor.server.plugins.DefaultHeaders
import io.ktor.server.plugins.ForwardedHeaderSupport
import io.ktor.server.plugins.HSTS
import io.ktor.server.plugins.StatusPages
import io.ktor.server.plugins.XForwardedHeaderSupport
import io.ktor.server.plugins.deflate
import io.ktor.server.plugins.gzip
import io.ktor.server.plugins.minimumSize
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.maxAge
import io.ktor.server.websocket.WebSockets
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.ontola.cache.assets.Assets
import io.ontola.cache.dataproxy.DataProxy
import io.ontola.cache.health.mountHealth
import io.ontola.cache.plugins.CSP
import io.ontola.cache.plugins.CacheConfig
import io.ontola.cache.plugins.CacheConfiguration
import io.ontola.cache.plugins.CacheSession
import io.ontola.cache.plugins.DeviceId
import io.ontola.cache.plugins.Logging
import io.ontola.cache.plugins.Redirect
import io.ontola.cache.plugins.RedisAdapter
import io.ontola.cache.plugins.ServiceRegistry
import io.ontola.cache.plugins.Storage
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.plugins.Versions
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.requestTimings
import io.ontola.cache.routes.mountBulk
import io.ontola.cache.routes.mountIndex
import io.ontola.cache.routes.mountLogout
import io.ontola.cache.routes.mountManifest
import io.ontola.cache.routes.mountMaps
import io.ontola.cache.routes.mountStatic
import io.ontola.cache.routes.mountTestingRoutes
import io.ontola.cache.sessions.RedisSessionStorage
import io.ontola.cache.sessions.SessionData
import io.ontola.cache.sessions.signedTransformer
import io.ontola.cache.statuspages.errorPage
import io.ontola.cache.studio.Studio
import io.ontola.cache.tenantization.Tenantization
import io.ontola.cache.util.configureCallLogging
import io.ontola.cache.util.isHtmlAccept
import io.ontola.cache.util.mountWebSocketProxy
import mu.KotlinLogging
import kotlin.collections.set
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

fun ApplicationRequest.isHTML(): Boolean {
    val accept = header(HttpHeaders.Accept) ?: ""
    val contentType = header(HttpHeaders.ContentType) ?: ""

    return accept.isHtmlAccept() || contentType.contains("text/html")
}

@OptIn(ExperimentalLettuceCoroutinesApi::class, ExperimentalTime::class)
@Suppress("unused") // Referenced in application.conf
@JvmOverloads
fun Application.module(
    testing: Boolean = false,
    storage: StorageAdapter<String, String>? = null,
    persistentStorage: StorageAdapter<String, String>? = null,
    client: HttpClient = createClient(),
) {
    Versions.print()
    val config = CacheConfig.fromEnvironment(environment.config, testing, client)
    config.print()
    val adapter = storage ?: RedisAdapter(RedisClient.create(config.redis.uri).connect().coroutines())
    val persistentAdapter = persistentStorage ?: RedisAdapter(RedisClient.create(config.persistentRedisURI).connect().coroutines())

    install(MicrometerMetrics) {
        registry = Metrics.metricsRegistry
    }

    install(Logging)

    install(CallLogging) {
        configureCallLogging()
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            when (val status = call.response.status()) {
                HttpStatusCode.Found -> "$status: ${call.request.toLogString()} -> ${call.response.headers[HttpHeaders.Location]}"
                else -> {
                    val ua = call.request.header("X-Forwarded-UA") ?: call.request.header("User-Agent") ?: "-"
                    val timings = call.requestTimings.joinToString { (name, time) -> "$name: ${time}ms" }
                    "$status - ${call.request.toLogString()} - timings: ($timings) - UA: $ua"
                }
            }
        }
    }

    install(HSTS) {
        maxAgeInSeconds = 365.days.inWholeSeconds
        includeSubDomains = true
    }

    install(StatusPages) {
        exception<TenantNotFoundException> { call, _ ->
            call.respondHtml(HttpStatusCode.NotFound) {
                errorPage(HttpStatusCode.NotFound)
            }
        }
        exception<BadGatewayException> { call, _ ->
            call.respondHtml(HttpStatusCode.BadGateway) {
                errorPage(HttpStatusCode.BadGateway)
            }
        }
        exception<AuthenticationException> { call, _ ->
            call.respondHtml(HttpStatusCode.Unauthorized) {
                errorPage(HttpStatusCode.Unauthorized)
            }
        }
        exception<AuthorizationException> { call, _ ->
            call.respondHtml(HttpStatusCode.Forbidden) {
                errorPage(HttpStatusCode.Forbidden)
            }
        }
    }

    install(CacheConfiguration) {
        this.config = config
    }

    install(IgnoreTrailingSlash)

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
        this.persistentAdapter = persistentAdapter
        this.expiration = config.cacheExpiration
    }

    install(Redirect)

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

    install(DefaultHeaders) {
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        header("X-Content-Type-Options", "nosniff")
        header("X-Engine", "Ktor")
        header("X-Frame-Options", "DENY")
        header("X-Powered-By", "Ontola")
        header("X-XSS-Protection", "1; mode=block")
        Versions.ServerVersion?.let {
            header("X-Server-Version", it)
        }
        Versions.ClientVersion?.let {
            header("X-Client-Version", it)
        }
    }

    install(ForwardedHeaderSupport)
    install(XForwardedHeaderSupport)

    install(Sessions) {
        cookie<SessionData>(
            name = "identity",
            storage = RedisSessionStorage(persistentAdapter, cacheConfig.redis),
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

    install(Studio)

    install(Tenantization) {
        blacklist = listOf(
            "/favicon.ico",
            "/link-lib/cache/clear",
            "/link-lib/cache/status",
            "/link-lib/cache/metrics",
            "/d/health",
            "/metrics",
            "/_testing",
            "/static/",
            "/assets/",
            "/f_assets/",
            "/__webpack_hmr"
        )
    }

    install(CSP)

    install(WebSockets)

    install(DataProxy) {
        val loginTransform = { req: ApplicationRequest ->
            val websiteIri = req.header("website-iri")

            URLBuilder("$websiteIri/oauth/token").apply {
                parameters.apply {
                    append("client_id", config.sessions.clientId)
                    append("client_secret", config.sessions.clientSecret)
                    append("grant_type", "password")
                    append("scope", "user")
                }
            }.build().fullPath
        }

        binaryPaths = listOf(
            "/assets/",
            "/media_objects/",
        )
        blindPostPaths = listOf(
            "/follows/",
        )
        excludedPaths = listOf(
            "/link-lib/bulk",
            "/_testing/setSession",
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
            "pdf",
        )
        transforms[Regex("^/login$")] = loginTransform
        transforms[Regex("^/[\\w/]*/login$")] = loginTransform
    }

    routing {
        if (testing) {
            mountTestingRoutes()
        }
        mountStatic()
        mountHealth()
        mountManifest()
        mountBulk()
        mountWebSocketProxy()
        mountLogout()
        mountMaps()
        mountIndex(client)
    }
}
