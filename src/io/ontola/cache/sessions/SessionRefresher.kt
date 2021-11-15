package io.ontola.cache.sessions

import com.auth0.jwt.JWT
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.ontola.cache.plugins.LibroSession
import io.ontola.cache.util.configureClientLogging
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

class SessionRefresher(private val configuration: LibroSession.Configuration) {
    suspend fun refresh(sessionId: String, session: LegacySession): LegacySession {
        val userToken = session.userToken ?: throw Exception("No userToken present")
        val refreshToken = session.refreshToken ?: throw Exception("No refreshToken present")
        val refreshResponse = refreshToken(userToken, refreshToken)
        val newSession = session.copy(
            refreshToken = refreshResponse.refreshToken,
            userToken = refreshResponse.accessToken,
        )
        storeSession(sessionId, newSession)

        return newSession
    }

    private suspend fun refreshToken(userToken: String, refreshToken: String): OIDCTokenResponse {
        val client = HttpClient(CIO) {
            install(Logging) {
                configureClientLogging()
            }
            install(JsonFeature)
        }
        val issuer = JWT.decode(userToken).issuer
        val url = Url("$issuer/oauth/token")

        return client.request("${configuration.oidcUrl}${url.fullPath}") {
            method = HttpMethod.Post
            headers {
                header("Accept", "application/json")
                header("Authorization", "Bearer $userToken")
                header("Content-Type", "application/json")
                header("X-Forwarded-Host", url.host)
                header("X-Forwarded-Proto", "https")
                header("X-Forwarded-Ssl", "on")
            }

            body = OIDCRequest.refreshRequest(
                configuration.oidcClientId,
                configuration.oidcClientSecret,
                refreshToken
            )
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    private suspend fun storeSession(sessionId: String, session: LegacySession) {
        configuration.adapter.set(sessionId, Json.encodeToString(session))
    }
}
