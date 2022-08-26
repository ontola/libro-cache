package tools.empathy.libro.server.sessions.oidc

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import mu.KotlinLogging
import tools.empathy.url.appendPath

class OIDCClient(
    private val client: HttpClient,
    private val clientName: String,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun getConfiguration(origin: Url): OpenIdConfiguration? {
        return try {
            val configuration = client
                .get(origin.appendPath(".well-known", "openid-configuration.json"))
                .body<OpenIdConfiguration>()

            logger.trace { "configuration for $origin: $configuration" }

            return configuration
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createApplication(config: OpenIdConfiguration, redirectUris: List<Url>): ClientCredentials {
        return client
            .post(config.registrationEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(
                    RegistrationRequest(
                        redirectUris = redirectUris,
                        clientName = clientName,
                        applicationType = "web",
                        scope = listOf("guest", "user", "staff"),
                    ),
                )
            }
            .body()
    }
}
