@file:UseSerializers(UrlSerializer::class)

package tools.empathy.libro.server.sessions.oidc

import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import tools.empathy.libro.server.util.UrlSerializer

@Serializable
data class RegistrationRequest(
    @SerialName("redirect_uris")
    val redirectUris: List<Url>,
    @SerialName("client_name")
    val clientName: String,
    @SerialName("application_type")
    val applicationType: String,
    val scope: List<String>?,
)
