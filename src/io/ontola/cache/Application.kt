package io.ontola.cache

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.features.logging.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.features.*
import io.lettuce.core.FlushMode
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.ontola.cache.features.*
import io.ontola.cache.features.tenant
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import org.slf4j.event.Level
import java.net.URLDecoder
import java.nio.charset.Charset
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

@OptIn(KtorExperimentalLocationsAPI::class, io.lettuce.core.ExperimentalLettuceCoroutinesApi::class,
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

    val client = HttpClient(CIO) {
        install(Auth) {}
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
        install(Logging) {
            level = LogLevel.HEADERS
        }
        install(UserAgent) { agent = "cache" }
    }

    suspend fun authorizeBulk(call: ApplicationCall, lang: String, resources: List<String>): List<SPIResourceResponseItem> {
        // TODO: Support direct bearer header for API requests
        val authorization = call.session.legacySession()
        val websiteIRI = call.tenant.websiteIRI
        val originalReq = call.request
        val prefix = websiteIRI.encodedPath.split("/").getOrNull(1)?.let { "/$it" } ?: ""

        val res: String = client.post {
            url(call.services.route("$prefix/spi/bulk"))
            contentType(ContentType.Application.Json)
            headers {
                if (!authorization?.userToken.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer ${authorization!!.userToken}")
                }

                header("Accept-Language", lang)
                header("Website-IRI", websiteIRI)

                header("Accept", ContentType.Application.Json)
                header("Content-Type", ContentType.Application.Json)
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

            body = SPIAuthorizeRequest(resources = resources.map { r ->
                SPIResourceRequestItem(
                    iri = r,
                    include = true,
                )
            })
        }

        return Json.decodeFromString(res)
    }

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

    install(LibroSession) {
        cookieNameLegacy = config.sessions.cookieNameLegacy
        signatureNameLegacy = config.sessions.signatureNameLegacy
        this.libroRedisConn = libroRedisConn
        jwtValidator = JWT.require(Algorithm.HMAC512(config.sessions.jwtEncryptionToken))
            .withClaim("application_id", config.sessions.clientId)
            .build()
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
        exception<AuthenticationException> {
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> {
            call.respond(HttpStatusCode.Forbidden)
        }
    }

    install(Tenantization) {
        blacklist = listOf(
            "/favicon.ico",
            "/link-lib/cache/clear",
            "/metrics",
            "/static/",
            "/assets/",
            "/f_assets/",
            "/__webpack_hmr"
        )
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

                println("Fetching ${requested.size} resources from cache")
                val lang = call.session.language() ?: config.defaultLanguage
                val prefixes = listOfNotNull(
                    config.redis.rootPrefix,
                    config.redis.cachePrefix,
                    config.redis.cacheEntryPrefix,
                ).toTypedArray()

                fun toCacheKey(iri: String): String = listOfNotNull(*prefixes, iri, lang).joinToString(config.redis.separator)
                fun fromCacheKey(key: String): String = key.split(config.redis.separator)[prefixes.size + 1]

                val entries = requested
                    .map { e -> toCacheKey(e.iri) }
                    .asFlow()
                    .map { key -> key to cacheRedisConn.hmget(key, "iri", "status", "cacheControl", "contents") }
                    .map { (key, hash) ->
                        val test = hash.fold(mutableMapOf<String, String>()) { e, h ->
                            h.ifHasValue { e[h.key] = it }
                            e
                        }

                        fromCacheKey(key) to test
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
                    .associateBy({it.first}, {it.second})
                    .toMutableMap()
                println("Fetched ${entries.size} resources from cache")

                val toRequest = requested.filter {
                    val entry = entries[it.iri]
                    entry == null || entry.cacheControl != CacheControl.Public || entry.contents.isNullOrEmpty()
                }

                if (toRequest.isNotEmpty()) {
                    println("Requesting ${toRequest.size} resources")
                    println("Requesting ${toRequest.joinToString(", ") { it.iri }}")
                    redisUpdate = authorizeBulk(call, lang, toRequest.map { e -> e.iri })
                        .map {
                            val entry = CacheEntry(
                                iri = it.iri,
                                status = HttpStatusCode.fromValue(it.status),
                                cacheControl = it.cache,
                                contents = it.body,
                            )
                            entries[it.iri] = entry

                            entry
                        }
                        .filter { it.cacheControl != CacheControl.Private }
                        .associateBy({ toCacheKey(it.iri) }, {
                            mapOf(
                                "iri" to it.iri,
                                "status" to it.status.value.toString(10),
                                "cacheControl" to it.cacheControl.toString(),
                                "contents" to it.contents.orEmpty(),
                            )
                        })
                } else {
                    println("All ${requested.size} resources in cache")
                }
                if (entries.size != requested.size) {
                    println("Requested ${requested.size}, serving ${entries.size}")
                } else {
                    val diffPos = requested.map { it.iri } - entries.map { it.key }
                    val diffNeg = entries.map { it.key } - requested.map { it.iri }
                    println("Request / serve diff ${diffPos + diffNeg}")
                }

                call.respondText(
                    entries.toList().joinToString("\n") { (_, r) -> "${statusCode(r.iri, r.status)}\n${r.contents ?: ""}" },
                    ContentType.parse("application/hex+x-ndjson"),
                )
            }
            println("Request took $hotMillis ms")

            val coldMillis = measureTimeMillis {
                // Write to cache
                if (redisUpdate !== null && redisUpdate!!.isNotEmpty()) {
                    println("Updating redis after responding (${redisUpdate!!.size} entries)")
                    redisUpdate!!.forEach {
                        cacheRedisConn.hset(it.key, it.value)
                    }
                }
            }
            println("Request took $coldMillis ms")
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

