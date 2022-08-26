package tools.empathy.libro.server.sessions

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

open class SessionException(override val message: String?) : Exception(message)

class InvalidGrantException(override val message: String?) : SessionException(message)

class InvalidClientException(override val message: String?) : SessionException(message)

suspend fun throwSessionException(response: HttpResponse) {
    val error = Json.decodeFromString<BackendErrorResponse>(response.body())
    logger.warn { "E: ${error.error} - ${error.code} - ${error.errorDescription}" }

    when (error.error) {
        "invalid_client" -> throw InvalidClientException(error.errorDescription)
        "invalid_grant" -> throw InvalidGrantException(error.errorDescription)
        else -> throw RuntimeException("Unexpected error type with status ${response.status}")
    }
}
