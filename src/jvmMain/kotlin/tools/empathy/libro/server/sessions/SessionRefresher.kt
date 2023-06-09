package tools.empathy.libro.server.sessions

import com.auth0.jwt.JWT
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import tools.empathy.libro.server.plugins.CacheSessionConfiguration
import tools.empathy.url.appendPath

@Serializable
data class OIDCTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("refresh_token")
    val refreshToken: String,
    val scope: String,
    @SerialName("created_at")
    val createdAt: Long,
)

@Serializable
data class OIDCRequest(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("client_secret")
    val clientSecret: String,
    @SerialName("grant_type")
    val grantType: String,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    val scope: String? = null,
) {
    companion object {
        fun guestRequest(clientId: String, clientSecret: String): OIDCRequest = OIDCRequest(
            clientId,
            clientSecret,
            grantType = "password",
            scope = "guest",
        )

        fun refreshRequest(clientId: String, clientSecret: String, refreshToken: String): OIDCRequest = OIDCRequest(
            clientId = clientId,
            clientSecret = clientSecret,
            grantType = "refresh_token",
            refreshToken = refreshToken,
        )
    }
}

class SessionRefresher(private val configuration: CacheSessionConfiguration) {
    private val logger = KotlinLogging.logger {}

    /**
     * Retrieve a new access token for the given session.
     */
    suspend fun refresh(session: SessionData): SessionData? {
        val userToken = session.credentials!!.accessToken
        val refreshToken = session.credentials.refreshToken
        return try {
            val refreshResponse = refreshToken(userToken, refreshToken)

            session.copy(
                credentials = TokenPair(
                    accessToken = refreshResponse.accessToken,
                    refreshToken = refreshResponse.refreshToken,
                ),
            )
        } catch (e: Exception) {
            logger.error(e.message)
            if (e !is InvalidGrantException) {
                configuration.libroConfig.notify(e)
            }
            if (e is InvalidClientException) {
                throw e
            }

            null
        }
    }

    private suspend fun refreshToken(userToken: String, refreshToken: String): OIDCTokenResponse {
        val issuer = JWT.decode(userToken).issuer
        val tokenUri = Url(issuer).appendPath("oauth", "token")
        val oidcServerSettings = configuration.oidcSettingsManager.getOrCreate()!!
        val oidcTokenUri = oidcServerSettings.accessTokenUrl

        val response = configuration.client.post(oidcTokenUri) {
            expectSuccess = false

            headers {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $userToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.XForwardedHost, tokenUri.host)
                header(HttpHeaders.XForwardedProto, "https")
                header("X-Forwarded-Ssl", "on")
            }

            setBody(
                OIDCRequest.refreshRequest(
                    oidcServerSettings.credentials.clientId,
                    oidcServerSettings.credentials.clientSecret,
                    refreshToken,
                ),
            )
        }

        if (response.status.value >= HttpStatusCode.BadRequest.value) {
            throwSessionException(response)
        }

        return response.body()
    }
}
