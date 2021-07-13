package io.ontola.cache

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.call.receive
import io.ktor.client.features.expectSuccess
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
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
import io.ktor.http.ContentType
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.fullPath
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.locations.post
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.path
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.lettuce.core.FlushMode
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.ontola.cache.features.CacheConfig
import io.ontola.cache.features.CacheConfiguration
import io.ontola.cache.features.LibroSession
import io.ontola.cache.features.Logging
import io.ontola.cache.features.ServiceRegistry
import io.ontola.cache.features.Tenantization
import io.ontola.cache.features.logger
import io.ontola.cache.features.services
import io.ontola.cache.features.session
import io.ontola.cache.features.tenant
import io.ontola.cache.util.KeyManager
import io.ontola.cache.util.scopeBlankNodes
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import org.slf4j.event.Level
import java.net.URLDecoder
import java.nio.charset.Charset
import kotlin.collections.set
import kotlin.system.measureTimeMillis

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

fun statusCode(iri: String, status: HttpStatusCode): String {
    val statment = buildJsonArray {
        add(iri)
        add("http://www.w3.org/2011/http#statusCode")
        add(status.value.toString(10))
        add("http://www.w3.org/2001/XMLSchema#integer")
        add("")
        add("http://purl.org/link-lib/meta")
    }

    return statment.toString()
}

fun HeadersBuilder.copy(header: String, req: ApplicationRequest) {
    req.header(header)?.let {
        set(header, it)
    }
}

suspend fun HttpRequestBuilder.initHeaders(call: ApplicationCall, lang: String) {
    // TODO: Support direct bearer header for API requests
    val authorization = call.session.legacySession()
    val websiteIRI = call.tenant.websiteIRI
    val originalReq = call.request

    headers {
        if (!authorization?.userToken.isNullOrBlank()) {
            header(HttpHeaders.Authorization, "Bearer ${authorization!!.userToken}")
        }

        header("Accept-Language", lang)
        header("Website-IRI", websiteIRI)

        copy("Accept-Language", originalReq)
        header("X-Forwarded-Host", originalReq.header("Host"))
        copy("X-Forwarded-Proto", originalReq)
        copy("X-Forwarded-Ssl", originalReq)
        copy("X-Request-Id", originalReq)

//                copy("Accept-Language", originalReq)
        copy("Origin", originalReq)
        copy("Referer", originalReq)
        copy("User-Agent", originalReq)
        copy("X-Forwarded-Host", originalReq)
        copy("X-Forwarded-Proto", originalReq)
        copy("X-Forwarded-Ssl", originalReq)
        copy("X-Real-Ip", originalReq)
        copy("X-Requested-With", originalReq)
        copy("X-Request-Id", originalReq)

        copy("X-Client-Ip", originalReq)
        copy("Client-Ip", originalReq)
        copy("Host", originalReq)
        copy("Forwarded", originalReq)
    }
}

@OptIn(
    KtorExperimentalLocationsAPI::class, io.lettuce.core.ExperimentalLettuceCoroutinesApi::class,
    io.ktor.util.KtorExperimentalAPI::class
)
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val config = CacheConfig.fromEnvironment(environment.config, testing)

    val cacheRedis = RedisClient.create(config.redis.uri)
    val cacheRedisConn = cacheRedis.connect().coroutines()

    val libroRedis = RedisClient.create(config.libroRedisURI)
    val libroRedisConn = libroRedis.connect().coroutines()

    val client = createClient(testing)

    suspend fun authorizeBulk(call: ApplicationCall, lang: String, resources: List<String>): List<SPIResourceResponseItem> {
        val prefix = call.tenant.websiteIRI.encodedPath.split("/").getOrNull(1)?.let { "/$it" } ?: ""

        val res: String = client.post {
            url(call.services.route("$prefix/spi/bulk"))
            contentType(ContentType.Application.Json)
            initHeaders(call, lang)
            headers {
                header("Accept", ContentType.Application.Json)
                header("Content-Type", ContentType.Application.Json)
            }
            body = SPIAuthorizeRequest(
                resources = resources.map { r ->
                    SPIResourceRequestItem(
                        iri = r,
                        include = true,
                    )
                }
            )
        }

        return Json.decodeFromString(res)
    }

    suspend fun authorizePlain(call: ApplicationCall, lang: String, resources: List<String>): List<SPIResourceResponseItem> {
        return resources
            .asFlow()
            .map {
                it to client.get<HttpResponse> {
                    url(call.services.route(Url(it).fullPath))
                    initHeaders(call, lang)
                    headers {
                        header("Accept", "application/hex+x-ndjson")
                    }
                    expectSuccess = false
                }
            }.map { (iri, response) ->
                SPIResourceResponseItem(
                    iri = iri,
                    status = response.status.value,
                    cache = CacheControl.Private,
                    language = lang,
                    body = response.receive()
                )
            }.toList()
    }

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

    install(Locations) {
    }

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(ForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy
    install(XForwardedHeaderSupport) // WARNING: for security, do not include this if not behind a reverse proxy

    install(ContentNegotiation) {
    }

    install(StatusPages) {
        exception<TenantNotFoundException> {
            call.respond(HttpStatusCode.NotFound)
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
            "/link-lib/cache/clear",
            "/link-lib/cache/status",
            "/metrics",
            "/static/",
            "/assets/",
            "/f_assets/",
            "/__webpack_hmr"
        )
        this.client = client
    }

    install(LibroSession) {
        cookieNameLegacy = config.sessions.cookieNameLegacy
        oidcUrl = config.sessions.oidcUrl
        oidcClientId = config.sessions.clientId
        oidcClientSecret = config.sessions.clientSecret
        signatureNameLegacy = config.sessions.signatureNameLegacy
        this.libroRedisConn = libroRedisConn
        jwtValidator = JWT.require(Algorithm.HMAC512(config.sessions.jwtEncryptionToken))
            .withClaim("application_id", config.sessions.clientId)
            .build()
    }

    // https://ktor.io/servers/features/https-redirect.html#testing
//    if (!testing) {
//        install(HttpsRedirect) {
//             The port to redirect to. By default 443, the default HTTPS port.
//            sslPort = 443
//             301 Moved Permanently, or 302 Found redirect.
//            permanentRedirect = true
//        }
//    }

    routing {
        get("/link-lib/cache/status") {
            call.respondText("UP", contentType = ContentType.Text.Plain)
        }

        get("/link-lib/cache/clear") {
            val test = cacheRedisConn.flushdb(FlushMode.ASYNC)

            call.respondText(test ?: "no message given", ContentType.Text.Plain, HttpStatusCode.OK)
        }

        post<Bulk> {
            var redisUpdate: Map<String, Map<String, String>>? = null

            val hotMillis = measureTimeMillis {
                val params = call.receiveParameters()
                val resources = params.getAll("resource[]")
                val requested = resources?.map { r -> CacheRequest(URLDecoder.decode(r, Charset.defaultCharset().name())) } ?: emptyList()
                // TODO: handle empty request

                call.logger.debug { "Fetching ${requested.size} resources from cache" }
                val lang = call.session.language() ?: config.defaultLanguage
                val keyManager = KeyManager(config)

                val entries = requested
                    .map { e -> keyManager.toKey(e.iri, lang) }
                    .asFlow()
                    .map { key -> key to cacheRedisConn.hmget(key, "iri", "status", "cacheControl", "contents") }
                    .map { (key, hash) ->
                        val test = hash.fold(mutableMapOf<String, String>()) { e, h ->
                            h.ifHasValue { e[h.key] = it }
                            e
                        }

                        keyManager.fromKey(key).first to test
                    }
                    .toList()
                    .filter { (_, hash) -> hash.containsKey("iri") }
                    .map { (key, hash) ->
                        key to CacheEntry(
                            iri = hash["iri"]!!,
                            status = HttpStatusCode.fromValue(hash["status"]!!.toInt()),
                            cacheControl = CacheControl.valueOf(hash["cacheControl"]!!),
                            contents = hash["contents"],
                        )
                    }
                    .associateBy({ it.first }, { it.second })
                    .toMutableMap()
                call.logger.debug { "Fetched ${entries.size} resources from cache" }

                val toRequest = requested.filter {
                    val entry = entries[it.iri]
                    entry == null || entry.cacheControl != CacheControl.Public || entry.contents.isNullOrEmpty()
                }

                if (toRequest.isNotEmpty()) {
                    call.logger.debug { "Requesting ${toRequest.size} resources" }
                    call.logger.trace { "Requesting ${toRequest.joinToString(", ") { it.iri }}" }

                    redisUpdate = toRequest
                        .groupBy { call.services.resolve(Url(it.iri).fullPath) }
                        .flatMap { (service, resources) ->
                            if (service.bulk) {
                                authorizeBulk(call, lang, resources.map { e -> e.iri })
                            } else {
                                authorizePlain(call, lang, resources.map { e -> e.iri })
                            }
                        }
                        .map {
                            val entry = CacheEntry(
                                iri = it.iri,
                                status = HttpStatusCode.fromValue(it.status),
                                cacheControl = it.cache,
                                contents = scopeBlankNodes(it.body),
                            )
                            entries[it.iri] = entry

                            entry
                        }
                        .filter { it.cacheControl != CacheControl.Private }
                        .associateBy(
                            { keyManager.toKey(it.iri, lang) },
                            {
                                mapOf(
                                    "iri" to it.iri,
                                    "status" to it.status.value.toString(10),
                                    "cacheControl" to it.cacheControl.toString(),
                                    "contents" to it.contents.orEmpty(),
                                )
                            }
                        )
                } else {
                    call.logger.debug("All ${requested.size} resources in cache")
                }
                if (entries.size != requested.size) {
                    call.logger.warn("Requested ${requested.size}, serving ${entries.size}")
                } else {
                    val diffPos = requested.map { it.iri } - entries.map { it.key }
                    val diffNeg = entries.map { it.key } - requested.map { it.iri }
                    call.logger.warn("Request / serve diff ${diffPos + diffNeg}")
                }

                call.respondText(
                    entries.toList().joinToString("\n") { (_, r) -> "${statusCode(r.iri, r.status)}\n${r.contents ?: ""}" },
                    ContentType.parse("application/hex+x-ndjson"),
                )
            }
            call.logger.info("Request took $hotMillis ms")

            val coldMillis = measureTimeMillis {
                // Write to cache
                redisUpdate?.let {
                    if (it.isNotEmpty()) {
                        call.logger.debug { "Updating redis after responding (${it.size} entries)" }
                        it.forEach { (key, value) ->
                            cacheRedisConn.hset(key, value)
                            config.cacheExpiration?.let { cacheExpiration ->
                                cacheRedisConn.expire(key, cacheExpiration)
                            }
                        }
                    }
                }
            }
            call.logger.info("Request took $coldMillis ms")
        }
    }
}

@Serializable
enum class CacheControl {
    @SerialName("none")
    None,
    @SerialName("public")
    Public,
    @SerialName("private")
    Private,
    @SerialName("no-cache")
    NoCache,
}

@Serializable
data class CacheRequest(
    val iri: String,
)

data class CacheEntry(
    val iri: String,
    val status: HttpStatusCode = HttpStatusCode.NotFound,
    val cacheControl: CacheControl = CacheControl.NoCache,
    val contents: String? = null,
)

@Serializable
data class SPIResourceResponseItem(
    val iri: String,
    val status: Int,
    val cache: CacheControl,
    val language: String? = null,
    val body: String? = null,
)

@Serializable
data class SPIResourceRequestItem(
    val iri: String,
    val include: Boolean,
)

@Serializable
data class SPIAuthorizeRequest(
    val resources: List<SPIResourceRequestItem>,
)

@Location("/link-lib/bulk")
class Bulk

data class BulkRequest(
    val resources: List<String>,
)

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
