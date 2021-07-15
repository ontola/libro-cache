package io.ontola.cache

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.features.UserAgent
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.http.hostWithPort
import io.ontola.cache.features.TenantFinderRequest
import io.ontola.cache.features.TenantFinderResponse
import io.ontola.cache.util.configureClientLogging
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.Charset

private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"

fun createClient(testing: Boolean): HttpClient {
    val configure: HttpClientConfig<*>.() -> Unit = {
        install(Auth) {}
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(Logging) {
            configureClientLogging()
        }
        install(UserAgent) { agent = "cache" }
    }

    return if (!testing) {
        HttpClient(CIO, configure)
    } else {
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
    }
}
