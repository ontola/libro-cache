@file:UseSerializers(UrlSerializer::class)

package tools.empathy.libro.server.sessions.oidc

import io.ktor.http.Url
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import tools.empathy.libro.server.util.UrlSerializer

@Serializable
data class OIDCServerSettings(
    val origin: Url,
    val authorizeUrl: Url,
    val accessTokenUrl: Url,
    val credentials: ClientCredentials,
)
