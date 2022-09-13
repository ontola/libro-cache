package tools.empathy.libro.server

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
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.oauth
import io.ktor.server.dynamicCookie.dynamicCookie
import io.ktor.server.html.respondHtml
import io.ktor.server.locations.Locations
import io.ktor.server.logging.toLogString
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.CachingHeaders
import io.ktor.server.plugins.CallId
import io.ktor.server.plugins.CallLogging
import io.ktor.server.plugins.Compression
import io.ktor.server.plugins.DefaultHeaders
import io.ktor.server.plugins.ForwardedHeaderSupport
import io.ktor.server.plugins.StatusPages
import io.ktor.server.plugins.XForwardedHeaderSupport
import io.ktor.server.plugins.callId
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
import kotlinx.coroutines.runBlocking
import tools.empathy.libro.server.bundle.Bundle
import tools.empathy.libro.server.configuration.LibroConfig
import tools.empathy.libro.server.configuration.LibroConfiguration
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.csp.CSP
import tools.empathy.libro.server.csp.cspReportEndpointPath
import tools.empathy.libro.server.csp.mountCSP
import tools.empathy.libro.server.dataproxy.DataProxyPlugin
import tools.empathy.libro.server.dataproxy.ProxyClient
import tools.empathy.libro.server.dataproxy.ProxyRule
import tools.empathy.libro.server.health.mountHealth
import tools.empathy.libro.server.plugins.Blacklist
import tools.empathy.libro.server.plugins.CSRFVerificationException
import tools.empathy.libro.server.plugins.CacheSession
import tools.empathy.libro.server.plugins.CsrfProtection
import tools.empathy.libro.server.plugins.DevelopmentSupport
import tools.empathy.libro.server.plugins.DeviceId
import tools.empathy.libro.server.plugins.HSTS
import tools.empathy.libro.server.plugins.LanguageNegotiation
import tools.empathy.libro.server.plugins.Logging
import tools.empathy.libro.server.plugins.Redirect
import tools.empathy.libro.server.plugins.RedisAdapter
import tools.empathy.libro.server.plugins.ServiceRegistry
import tools.empathy.libro.server.plugins.SessionLanguage
import tools.empathy.libro.server.plugins.Storage
import tools.empathy.libro.server.plugins.StorageAdapter
import tools.empathy.libro.server.plugins.StoragePlugin
import tools.empathy.libro.server.plugins.Versions
import tools.empathy.libro.server.plugins.language
import tools.empathy.libro.server.plugins.logger
import tools.empathy.libro.server.plugins.managementTenant
import tools.empathy.libro.server.plugins.requestTimings
import tools.empathy.libro.server.routes.mountBulk
import tools.empathy.libro.server.routes.mountIndex
import tools.empathy.libro.server.routes.mountLogout
import tools.empathy.libro.server.routes.mountManifest
import tools.empathy.libro.server.routes.mountMaps
import tools.empathy.libro.server.routes.mountStatic
import tools.empathy.libro.server.routes.mountTestingRoutes
import tools.empathy.libro.server.sessions.OIDCSession
import tools.empathy.libro.server.sessions.RedisSessionStorage
import tools.empathy.libro.server.sessions.SessionData
import tools.empathy.libro.server.sessions.mountLogin
import tools.empathy.libro.server.sessions.oidc.OIDCSettingsManager
import tools.empathy.libro.server.sessions.signedTransformer
import tools.empathy.libro.server.statuspages.RenderLanguage
import tools.empathy.libro.server.statuspages.errorPage
import tools.empathy.libro.server.tenantization.TenantData
import tools.empathy.libro.server.tenantization.Tenantization
import tools.empathy.libro.server.util.KeyManager
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.libro.server.util.configure
import tools.empathy.libro.server.util.configureCallLogging
import tools.empathy.libro.server.util.configureClientLogging
import tools.empathy.libro.server.util.isHtmlAccept
import tools.empathy.libro.server.util.mountWebSocketProxy
import tools.empathy.libro.server.util.origin
import tools.empathy.studio.Studio
import tools.empathy.studio.StudioDeploymentKey
import tools.empathy.studio.mountStudio
import tools.empathy.studio.studioManifest
import tools.empathy.url.appendPath
import tools.empathy.url.disableCertValidation
import java.util.UUID
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
// Referenced in application.conf
@Suppress("unused")
@JvmOverloads
fun Application.module(
    testing: Boolean = false,
    storage: StorageAdapter<String, String>? = null,
    persistentStorage: StorageAdapter<String, String>? = null,
    client: HttpClient? = null,
) {
    Versions.print()
    val config = LibroConfig.fromEnvironment(environment.config, testing, client)
    config.print()
    val adapter = storage ?: RedisAdapter(RedisClient.create(config.redis.uri).connect().coroutines())
    val persistentAdapter =
        persistentStorage ?: RedisAdapter(RedisClient.create(config.persistentRedisURI).connect().coroutines())
    val oidcManager = OIDCSettingsManager(config, Storage(persistentAdapter, KeyManager(config.redis), expiration = null))

    install(MicrometerMetrics) {
        registry = Metrics.metricsRegistry
    }

    install(CallId) {
        generate { UUID.randomUUID().toString() }
    }
    install(Logging)

    install(CallLogging) {
        mdc("callid") { call ->
            call.callId
        }
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

        withHost(config.studio.domain) {
            includeSubDomains = false
        }
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

    install(LibroConfiguration) {
        this.config = config
    }

    install(DevelopmentSupport)

    install(IgnoreTrailingSlash)

    install(Bundle)

    install(ServiceRegistry) {
        if (testing) {
            initFromTest(config.services)
        } else {
            initFrom(config.services)
        }
    }

    install(StoragePlugin) {
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
                ContentType.Application.FontWoff,
                -> CachingOptions(
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
            header(LibroHttpHeaders.XServerVersion, it)
        }
        Versions.ClientVersion?.let {
            header(LibroHttpHeaders.XClientVersion, it)
        }
    }

    install(ForwardedHeaderSupport)
    install(XForwardedHeaderSupport)

    install(Blacklist) {
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
            "/libro/docs/",
            "/__webpack_hmr",
            "/.well-known/openid-configuration",
            "/oauth/authorize",
            "/oauth/discovery/keys",
            "/oauth/introspect",
            "/oauth/register",
            "/oauth/revoke",
            "/oauth/token",
            "/oauth/userinfo",
        )
    }

    install(Tenantization) {
        val management = managementTenant(
            libroConfig.management.origin,
            libroConfig.devClientPort,
        )

        staticTenants = mapOf(
            management.websiteOrigin.host to management,
            config.studio.domain to TenantData.Local(
                name = "Studio",
                websiteIRI = config.studio.origin,
                websiteOrigin = config.studio.origin,
                manifest = studioManifest(config.studio.origin),
                context = { attributes[StudioDeploymentKey] },
            ),
        )
    }

    install(Studio) {
        blacklist = listOf(
            "/f_assets/",
        )
    }

    install(Sessions) {
        dynamicCookie<SessionData>(
            name = config.sessions.cookieName,
            storage = RedisSessionStorage(persistentAdapter, libroConfig.redis),
        ) {
            cookie.configure()
            if (!testing && !config.isDev) {
                transform(
                    signedTransformer(signingSecret = config.sessions.sessionSecret),
                )
            }
        }
        cookie<OIDCSession>(name = "oidc_identity") {
            cookie.httpOnly = true
            cookie.secure = true
            cookie.extensions["SameSite"] = "Lax"
            if (!testing && !config.isDev) {
                transform(
                    signedTransformer(signingSecret = config.sessions.sessionSecret),
                )
            }
        }
        cookie<String>("deviceId") {
            cookie.httpOnly = true
            cookie.secure = true
            cookie.extensions["SameSite"] = "Lax"
            cookie.maxAge = 365.days
            transform(
                signedTransformer(signingSecret = config.sessions.sessionSecret),
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
        this.oidcSettingsManager = oidcManager
        val jwtToken = Algorithm.HMAC512(config.sessions.jwtEncryptionToken)
        jwtValidator = JWT.require(jwtToken).build()
    }

    install(Authentication) {
        oauth("libro-oidc") {
            this.client = config.client

            urlProvider = {
                Url(request.origin()).appendPath("libro", "callback").toString()
            }
            providerLookup = {
                val origin = Url(request.origin())
                val redirectUris = listOf(origin.appendPath("libro", "callback"))
                runBlocking { oidcManager.getOrCreate(origin, redirectUris) }?.let {
                    OAuthServerSettings.OAuth2ServerSettings(
                        name = "oidc-${it.origin}",
                        authorizeUrl = it.authorizeUrl.toString(),
                        accessTokenUrl = it.accessTokenUrl.toString(),
                        requestMethod = HttpMethod.Post,
                        clientId = it.credentials.clientId,
                        clientSecret = it.credentials.clientSecret,
                        defaultScopes = listOf("user", "staff"),
                    )
                }
            }
        }
    }

    install(CSP)

    install(CsrfProtection) {
        blackList = listOf(
            Regex("^/_testing"),
            Regex("^/csp-reports"),
            Regex("^/([\\w/]*/)?link-lib/bulk"),
            Regex("^/_studio/"),
            Regex("^/([\\w/]*/)?follows/"),
            Regex("^/oauth/register"),
            Regex("^/oauth/revoke"),
            Regex("^/oauth/token"),
            Regex("^/([\\w/]*/)?active_storage/disk"),
        )
    }

    install(WebSockets)

    install(DataProxyPlugin) {
        val loginTransform = { req: ApplicationRequest ->
            val websiteIri = req.header("website-iri")

            URLBuilder(Url(websiteIri!!).appendPath("oauth", "token")).apply {
                parameters.apply {
                    if (oidcManager.getOrCreateBlocking() == null) {
                        logger.warn { "No OIDC settings, omitting credentials from login transformation" }
                    }
                    oidcManager.getOrCreateBlocking()?.let {
                        append("client_id", it.credentials.clientId)
                        append("client_secret", it.credentials.clientSecret)
                    }
                    append("grant_type", "password")
                    append("scope", "user")
                }
            }.build().fullPath
        }

        rules = listOf(
            ProxyRule(Regex("^/([\\w/]*/)?manifest.json$")),

            ProxyRule(Regex("^/.well-known/openid-configuration$")),
            ProxyRule(Regex("^/.well-known/webfinger(\\?.*)?")),
            ProxyRule(Regex("^/oauth/authorize(\\?.*)?")),
            ProxyRule(Regex("^/oauth/discovery/keys")),
            ProxyRule(Regex("^/oauth/introspect")),
            ProxyRule(Regex("^/oauth/userinfo")),
            ProxyRule(Regex("^/oauth/token")),

            ProxyRule(Regex("/media_objects/\\w+/content"), client = ProxyClient.RedirectingBackend),
            ProxyRule(Regex("/active_storage/"), client = ProxyClient.RedirectingBackend),

            ProxyRule(Regex("/assets/"), client = ProxyClient.Binary, includeCredentials = false),
            ProxyRule(Regex("/photos/"), client = ProxyClient.Binary),

            ProxyRule(Regex("^/_testing/setSession"), exclude = true),
            ProxyRule(Regex("^$cspReportEndpointPath"), exclude = true),
            ProxyRule(Regex("^/d/health"), exclude = true),
            ProxyRule(Regex("^/_studio/"), exclude = true),
            ProxyRule(Regex("^/([\\w/]*/)?link-lib/bulk"), exclude = true),
            ProxyRule(Regex("^/([\\w/]*/)?libro/login"), exclude = true),
            ProxyRule(Regex("^/([\\w/]*/)?logout"), exclude = true),
            ProxyRule(Regex("/static/"), exclude = true),
        )

        contentTypes = listOf(
            ContentType.parse("application/hex+x-ndjson"),
            ContentType.parse("application/json"),
            ContentType.parse("application/empathy+json"),
            ContentType.parse("application/ld+json"),
            ContentType.parse("application/n-quads"),
            ContentType.parse("application/n-triples"),
            ContentType.parse("text/turtle"),
            ContentType.parse("text/n3"),
        )
        extensions = listOf(
            "hndjson",
            "empjson",
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
            HttpMethod.Options,
        )
        transforms[Regex("^/([\\w/]*/)?login$")] = loginTransform

        developmentMode = testing || this@module.libroConfig.isDev

        binaryClient = HttpClient(CIO) {
            followRedirects = true
            expectSuccess = false
            developmentMode = testing || this@module.libroConfig.isDev
            install(io.ktor.client.plugins.logging.Logging) {
                configureClientLogging()
            }
            if (this@install.developmentMode) {
                disableCertValidation()
                engine {
                    requestTimeout = 1.days.inWholeMilliseconds
                }
            }
        }
    }

    install(SessionLanguage)

    routing {
        if (testing) {
            mountTestingRoutes()
        }
        mountStatic(config.isDev)
        mountHealth()
        mountCSP()
        mountStudio()
        mountManifest()
        mountBulk()
        mountWebSocketProxy()
        mountLogin()
        mountLogout()
        mountMaps()
        mountIndex()
    }
}
