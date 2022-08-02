package tools.empathy.libro.server.sessions.oidc

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import tools.empathy.url.appendPath

class OIDCClient(
    private val client: HttpClient,
    private val clientName: String,
) {
    suspend fun getConfiguration(origin: Url): OpenIdConfiguration? {
        return try {
            client
                .get(origin.appendPath(".well-known", "openid-configuration.json"))
                .body()
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
                        scope = listOf("guest", "user", "staff")
                    )
                )
            }
            .body()
    }
}
