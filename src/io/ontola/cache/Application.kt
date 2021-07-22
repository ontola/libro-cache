package io.ontola.cache

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.ForwardedHeaderSupport
import io.ktor.features.StatusPages
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.features.deflate
import io.ktor.features.gzip
import io.ktor.features.minimumSize
import io.ktor.features.toLogString
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.timeout
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Locations
import io.ktor.locations.post
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.ontola.cache.bulk.CacheRequest
import io.ontola.cache.bulk.bulkHandler
import io.ontola.cache.bulk.socketHandler
import io.ontola.cache.plugins.CacheConfig
import io.ontola.cache.plugins.CacheConfiguration
import io.ontola.cache.plugins.LibroSession
import io.ontola.cache.plugins.Logging
import io.ontola.cache.plugins.RedisAdapter
import io.ontola.cache.plugins.ServiceRegistry
import io.ontola.cache.plugins.Storage
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.plugins.Tenantization
import io.ontola.cache.plugins.requestTimings
import io.ontola.cache.plugins.session
import io.ontola.cache.plugins.setTenantFromWebsiteIRI
import io.ontola.cache.plugins.storage
import io.ontola.cache.plugins.tenant
import io.ontola.cache.sessions.createJWTVerifier
import io.ontola.cache.util.configureCallLogging
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@OptIn(
    KtorExperimentalLocationsAPI::class,
    io.lettuce.core.ExperimentalLettuceCoroutinesApi::class,
    io.ktor.http.cio.websocket.ExperimentalWebSocketExtensionApi::class
)
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(
    testing: Boolean = false,
    storage: StorageAdapter<String, String>? = null,
    client: HttpClient = createClient(),
) {
    val config = CacheConfig.fromEnvironment(environment.config, testing, client)

    install(Logging)

    install(CacheConfiguration) {
        this.config = config
    }

    install(ServiceRegistry) {
        if (testing) {
            initFromTest(config.services)
        } else {
            initFrom(config.services)
        }
    }

    install(Storage) {
        this.adapter = storage ?: RedisAdapter(RedisClient.create(config.redis.uri).connect().coroutines())
        this.expiration = config.cacheExpiration
    }

    install(Locations)

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE // Disabled (max value). The connection will be closed if surpassed this length.
        masking = false

//        extensions {
//            install(WebSocketDeflateExtension) {
//                compressionLevel = Deflater.DEFAULT_COMPRESSION
//                compressIfBiggerThan(bytes = 4 * 1024)
//            }
//        }
    }

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
        header("X-Powered-By", "Ontola") // will send this header with each response
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(ForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy
    install(XForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy

    install(ContentNegotiation)

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

    install(Tenantization) {
        blacklist = listOf(
            "/favicon.ico",
            "/link-lib/socket",
            "/link-lib/cache/clear",
            "/link-lib/cache/status",
            "/metrics",
            "/static/",
            "/assets/",
            "/f_assets/",
            "/__webpack_hmr"
        )
    }

    install(LibroSession) {
        adapter = RedisAdapter(RedisClient.create(config.libroRedisURI).connect().coroutines())
        cookieNameLegacy = config.sessions.cookieNameLegacy
        oidcUrl = config.sessions.oidcUrl
        oidcClientId = config.sessions.clientId
        oidcClientSecret = config.sessions.clientSecret
        signatureNameLegacy = config.sessions.signatureNameLegacy
        jwtValidator = createJWTVerifier(config.sessions.jwtEncryptionToken, config.sessions.clientId)
    }

    routing {
        webSocket("/link-lib/socket") {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        if (text.startsWith("website-iri:")) {
                            text.split("website-iri:").last().let {
                                call.setTenantFromWebsiteIRI(Url(it))
                            }
                            outgoing.send(Frame.Text("OK"))
                            continue
                        } else if (text.equals("bye", ignoreCase = true)) {
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        } else if (text.startsWith("request: ")) {
                            val requested = text
                                .split("request: ")
                                .last()
                                .split(", ")
                                .map { CacheRequest(it) }

                            call.socketHandler(requested, outgoing)
                        } else {
                            val name = call.session.claimsFromJWT()?.user?.id ?: "unknown"
                            outgoing.send(Frame.Text("HELLO $name YOU SAID: $text in ${call.session.language()} from ${call.tenant.websiteIRI}"))
                        }
                    }
                }
            }
        }

        get("/link-lib/cache/status") {
            call.respondText("UP", contentType = ContentType.Text.Plain)
        }

        get("/link-lib/cache/clear") {
            val test = call.application.storage.clear()

            call.respondText(test ?: "no message given", ContentType.Text.Plain, HttpStatusCode.OK)
        }

        post(bulkHandler())
    }
}
