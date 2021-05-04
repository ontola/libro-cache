package io.ontola.cache

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.http.*
import io.ktor.util.*
import io.ontola.cache.features.TenantFinderRequest
import io.ontola.cache.features.TenantFinderResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.Charset

private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"

@OptIn(KtorExperimentalAPI::class)
fun createClient(testing: Boolean): HttpClient {
    val configure: HttpClientConfig<*>.() -> Unit =  {
        install(Auth) {}
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(Logging) {
            level = LogLevel.HEADERS
        }
        install(UserAgent) { agent = "cache" }
    }

    return if (testing) {
        HttpClient(MockEngine) {
            configure()
            engine {
                addHandler { request ->
                    when (request.url.fullUrl) {
                        "https://example.org/" -> {
                            val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Text.Plain.toString()))
                            respond("Hello, world", headers = responseHeaders)
                        }
                        "https://data.local/_public/spi/find_tenant" -> {
                            val body = request.body.toByteArray().toString(Charset.defaultCharset())
                            val requestPayload = Json.decodeFromString<TenantFinderRequest>(body)
                            val payload = TenantFinderResponse(
                                iriPrefix = requestPayload.iri
                            )
                            val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                            respond(
                                Json.encodeToString(payload),
                                HttpStatusCode.OK,
                                headers = responseHeaders,
                            )
                        }
                        else -> error("Unhandled ${request.url.fullUrl}")
                    }
                }
            }
        }
    } else {
        HttpClient(CIO, configure)
    }
}
