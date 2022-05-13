package io.ontola.cache.sessions

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
import io.ktor.http.fullPath
import io.ontola.cache.plugins.CacheSessionConfiguration
import io.ontola.util.appendPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

class InvalidGrantException : Exception()

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
            scope = "guest"
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
                configuration.cacheConfig.notify(e)
            }
            null
        }
    }

    private suspend fun refreshToken(userToken: String, refreshToken: String): OIDCTokenResponse {
        val issuer = JWT.decode(userToken).issuer
        val tokenUri = Url(issuer).appendPath("oauth", "token")
        val oidcTokenUri = configuration.oidcUrl.appendPath(tokenUri.fullPath)

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
                    configuration.oidcClientId,
                    configuration.oidcClientSecret,
                    refreshToken
                )
            )
        }

        if (response.status == HttpStatusCode.BadRequest) {
            val error = Json.decodeFromString<BackendErrorResponse>(response.body())
            logger.warn { "E: ${error.error} - ${error.code} - ${error.errorDescription}" }
            if (error.error == "invalid_grant") {
                throw InvalidGrantException()
            } else {
                throw RuntimeException("Unexpected body with status 400")
            }
        }

        return response.body()
    }
}
