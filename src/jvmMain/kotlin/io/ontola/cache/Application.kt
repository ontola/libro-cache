package io.ontola.cache

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
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
import io.ontola.cache.csp.CSP
import io.ontola.cache.csp.cspReportEndpointPath
import io.ontola.cache.csp.mountCSP
import io.ontola.cache.dataproxy.DataProxy
import io.ontola.cache.health.mountHealth
import io.ontola.cache.plugins.CSRFVerificationException
import io.ontola.cache.plugins.CacheConfig
import io.ontola.cache.plugins.CacheConfiguration
import io.ontola.cache.plugins.CacheSession
import io.ontola.cache.plugins.CsrfProtection
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
import io.ontola.cache.util.configureClientLogging
import io.ontola.cache.util.isHtmlAccept
import io.ontola.cache.util.mountWebSocketProxy
import io.ontola.util.appendPath
import io.ontola.util.disableCertValidation
import kotlin.collections.set
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

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
        includeSubDomains = true
    }

    install(StatusPages) {
        exception<TenantNotFoundException> { call, cause ->
            call.respondHtml(HttpStatusCode.NotFound) {
                errorPage(HttpStatusCode.NotFound, cause)
            }
        }
        exception<BadGatewayException> { call, cause ->
            call.respondHtml(HttpStatusCode.BadGateway) {
                errorPage(HttpStatusCode.BadGateway, cause)
            }
        }
        exception<AuthenticationException> { call, cause ->
            call.respondHtml(HttpStatusCode.Unauthorized) {
                errorPage(HttpStatusCode.Unauthorized, cause)
            }
        }
        exception<AuthorizationException> { call, cause ->
            call.respondHtml(HttpStatusCode.Forbidden) {
                errorPage(HttpStatusCode.Forbidden, cause)
            }
        }
        exception<CSRFVerificationException> { call, cause ->
            call.respondHtml(HttpStatusCode.Forbidden) {
                errorPage(HttpStatusCode.Forbidden, cause)
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
        header("X-Frame-Options", "DENY")
        header("X-Powered-By", "Ontola")
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
            cookie.extensions["SameSite"] = "Strict"
        }
        cookie<String>("deviceId") {
            cookie.httpOnly = true
            cookie.secure = true
            cookie.extensions["SameSite"] = "Strict"
            cookie.maxAge = 365.days
            transform(
                signedTransformer(signingSecret = config.sessions.sessionSecret)
            )
        }
    }

    install(DeviceId) {
        blacklist = listOf(
            "/favicon.ico",
            "/link-lib/cache/clear",
            "/link-lib/cache/status",
            "/link-lib/cache/metrics",
            "/d/health",
            "/metrics",
            "/_testing",
            "/csp-reports",
            "/__webpack_hmr",
        )
    }

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
            cspReportEndpointPath,
            "/favicon.ico",
            "/link-lib/cache/clear",
            "/link-lib/cache/status",
            "/link-lib/cache/metrics",
            "/d/health",
            "/metrics",
            "/_testing",
            "/csp-reports",
            "/static/",
            "/assets/",
            "/photos/",
            "/f_assets/",
            "/__webpack_hmr",
        )
    }

    install(CSP)

    install(CsrfProtection) {
        blackList = listOf(
            "/_testing",
            "/csp-reports",
            "/link-lib/bulk",
            "/follows/",
        )
    }

    install(WebSockets)

    install(DataProxy) {
        val loginTransform = { req: ApplicationRequest ->
            val websiteIri = req.header("website-iri")

            URLBuilder(Url(websiteIri!!).appendPath("oauth", "token")).apply {
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
            "/photos/",
        )
        excludedPaths = listOf(
            Regex("^/_testing/setSession"),
            Regex("^$cspReportEndpointPath"),
            Regex("^/d/health"),
            Regex("^/link-lib/bulk"),
            Regex("^/([\\w/]*/)?logout"),
            Regex("/static/"),
        )
        includedPaths = listOf(
            "/media_objects/",
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
        methods = listOf(
            HttpMethod.Post,
            HttpMethod.Put,
            HttpMethod.Patch,
            HttpMethod.Delete,
        )
        transforms[Regex("^/([\\w/]*/)?login$")] = loginTransform

        binaryClient = HttpClient(CIO) {
            followRedirects = true
            expectSuccess = false
            developmentMode = testing || this@module.cacheConfig.isDev
            install(io.ktor.client.plugins.logging.Logging) {
                configureClientLogging()
            }
            if (testing || this@module.cacheConfig.isDev) disableCertValidation()
        }
    }

    routing {
        if (testing) {
            mountTestingRoutes()
        }
        mountStatic()
        mountHealth()
        mountCSP()
        mountManifest()
        mountBulk()
        mountWebSocketProxy()
        mountLogout()
        mountMaps()
        mountIndex(client)
    }
}
