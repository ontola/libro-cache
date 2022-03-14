package io.ontola.cache

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.content.CachingOptions
import io.ktor.http.fullPath
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.html.respondHtml
import io.ktor.server.locations.KtorExperimentalLocationsAPI
import io.ktor.server.locations.Locations
import io.ktor.server.logging.toLogString
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.CachingHeaders
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
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.assets.Assets
import io.ontola.cache.csp.CSP
import io.ontola.cache.csp.cspReportEndpointPath
import io.ontola.cache.csp.mountCSP
import io.ontola.cache.dataproxy.DataProxy
import io.ontola.cache.dataproxy.ProxyClient
import io.ontola.cache.dataproxy.ProxyRule
import io.ontola.cache.health.mountHealth
import io.ontola.cache.plugins.CSRFVerificationException
import io.ontola.cache.plugins.CacheConfig
import io.ontola.cache.plugins.CacheConfiguration
import io.ontola.cache.plugins.CacheSession
import io.ontola.cache.plugins.CsrfProtection
import io.ontola.cache.plugins.DeviceId
import io.ontola.cache.plugins.LanguageNegotiation
import io.ontola.cache.plugins.Logging
import io.ontola.cache.plugins.Redirect
import io.ontola.cache.plugins.RedisAdapter
import io.ontola.cache.plugins.ServiceRegistry
import io.ontola.cache.plugins.Storage
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.plugins.Versions
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.language
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
import io.ontola.cache.statuspages.RenderLanguage
import io.ontola.cache.statuspages.errorPage
import io.ontola.cache.tenantization.TenantData
import io.ontola.cache.tenantization.Tenantization
import io.ontola.cache.util.configureCallLogging
import io.ontola.cache.util.configureClientLogging
import io.ontola.cache.util.isHtmlAccept
import io.ontola.cache.util.mountWebSocketProxy
import io.ontola.studio.Studio
import io.ontola.studio.mountStudio
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

@OptIn(ExperimentalLettuceCoroutinesApi::class, ExperimentalTime::class, KtorExperimentalLocationsAPI::class)
@Suppress("unused") // Referenced in application.conf
@JvmOverloads
fun Application.module(
    testing: Boolean = false,
    storage: StorageAdapter<String, String>? = null,
    persistentStorage: StorageAdapter<String, String>? = null,
    client: HttpClient? = null,
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

    install(LanguageNegotiation) {
        defaultLanguage = config.defaultLanguage
    }

    install(HSTS) {
        includeSubDomains = true
    }

    install(StatusPages) {
        fun ApplicationCall.renderLanguage(): RenderLanguage = when {
            language.contains("nl") -> RenderLanguage.NL
            language.contains("en") -> RenderLanguage.EN
            language.contains("de") -> RenderLanguage.DE
            else -> RenderLanguage.NL
        }

        exception<TenantNotFoundException> { call, cause ->
            call.respondHtml(HttpStatusCode.NotFound) {
                errorPage(HttpStatusCode.NotFound, cause, call.renderLanguage())
            }
        }
        exception<BadGatewayException> { call, cause ->
            call.respondHtml(HttpStatusCode.BadGateway) {
                errorPage(HttpStatusCode.BadGateway, cause, call.renderLanguage())
            }
        }
        exception<AuthenticationException> { call, cause ->
            call.respondHtml(HttpStatusCode.Unauthorized) {
                errorPage(HttpStatusCode.Unauthorized, cause, call.renderLanguage())
            }
        }
        exception<AuthorizationException> { call, cause ->
            call.respondHtml(HttpStatusCode.Forbidden) {
                errorPage(HttpStatusCode.Forbidden, cause, call.renderLanguage())
            }
        }
        exception<CSRFVerificationException> { call, cause ->
            call.respondHtml(HttpStatusCode.Forbidden) {
                errorPage(HttpStatusCode.Forbidden, cause, call.renderLanguage())
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

    install(CachingHeaders) {
        options { outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Text.CSS,
                ContentType.Application.JavaScript,
                ContentType.Application.FontWoff -> CachingOptions(
                    CacheControl.MaxAge(maxAgeSeconds = 365.days.inWholeSeconds.toInt()),
                )
                else -> null
            }
        }
    }

    install(DefaultHeaders) {
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("Service-Worker-allowed", "/")
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
            cookie.extensions["SameSite"] = "Lax"
            if (!testing) {
                transform(
                    signedTransformer(signingSecret = config.sessions.sessionSecret)
                )
            }
        }
        cookie<String>("deviceId") {
            cookie.httpOnly = true
            cookie.secure = true
            cookie.extensions["SameSite"] = "Lax"
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

    install(Tenantization) {
        blacklist = listOf(
            cspReportEndpointPath,
            "/favicon.ico",
            "/link-lib/cache/clear",
            "/link-lib/cache/status",
            "/link-lib/cache/metrics",
            "/api/maps/",
            "/d/health",
            "/metrics",
            "/_testing",
            "/csp-reports",
            "/static/",
            "/assets/",
            "/photos/",
            "/f_assets/",
            "/__webpack_hmr",
            "/.well-known/openid-configuration",
        )

        staticTenants = mapOf(
            config.studio.domain to TenantData(
                client = cacheConfig.client,
                isBlackListed = false,
                websiteIRI = config.studio.origin,
                websiteOrigin = config.studio.origin,
                manifest = Manifest.forWebsite(config.studio.origin),
            )
        )
    }

    install(CSP)

    install(CsrfProtection) {
        blackList = listOf(
            Regex("^/_testing"),
            Regex("^/csp-reports"),
            Regex("^/link-lib/bulk"),
            Regex("^/_studio/"),
            Regex("^/([\\w/]*/)?follows/"),
            Regex("^/oauth/token"),
            Regex("^/oauth/register"),
        )
    }

    install(Studio)

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

        rules = listOf(
            ProxyRule(Regex("^/([\\w/]*/)?manifest.json$")),

            ProxyRule(Regex("^/.well-known/openid-configuration$")),
            ProxyRule(Regex("^/.well-known/webfinger(\\?.*)?")),
            ProxyRule(Regex("^/oauth/discovery/keys")),
            ProxyRule(Regex("^/oauth/userinfo")),

            ProxyRule(Regex("/media_objects/\\w+/content"), client = ProxyClient.RedirectingBackend),
            ProxyRule(Regex("/active_storage/"), client = ProxyClient.RedirectingBackend),

            ProxyRule(Regex("/assets/"), client = ProxyClient.Binary, includeCredentials = false),
            ProxyRule(Regex("/photos/"), client = ProxyClient.Binary),

            ProxyRule(Regex("^/_testing/setSession"), exclude = true),
            ProxyRule(Regex("^$cspReportEndpointPath"), exclude = true),
            ProxyRule(Regex("^/d/health"), exclude = true),
            ProxyRule(Regex("^/_studio/"), exclude = true),
            ProxyRule(Regex("^/link-lib/bulk"), exclude = true),
            ProxyRule(Regex("^/([\\w/]*/)?logout"), exclude = true),
            ProxyRule(Regex("/static/"), exclude = true),
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

        skipCertificateValidation = testing || this@module.cacheConfig.isDev

        binaryClient = HttpClient(CIO) {
            followRedirects = true
            expectSuccess = false
            developmentMode = testing || this@module.cacheConfig.isDev
            install(io.ktor.client.plugins.logging.Logging) {
                configureClientLogging()
            }
            if (skipCertificateValidation) disableCertValidation()
        }
    }

    routing {
        if (testing) {
            mountTestingRoutes()
        }
        mountStatic()
        mountHealth()
        mountCSP()
        mountStudio()
        mountManifest()
        mountBulk()
        mountWebSocketProxy()
        mountLogout()
        mountMaps()
        mountIndex()
    }
}
