@file:UseSerializers(UrlSerializer::class)

package tools.empathy.libro.server.sessions.oidc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import tools.empathy.libro.server.util.UrlSerializer

@Serializable
data class RegistrationResponse(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("client_secret")
    val clientSecret: String,
    @SerialName("client_secret_expires_at")
    val clientSecretExpiresAt: Long,
)
