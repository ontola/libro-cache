package io.ontola.cache.sessions

import com.auth0.jwt.JWT
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.ontola.cache.features.LibroSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class OIDCTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Long,
    val refresh_token: String,
    val scope: String,
    val created_at: Long,
)

@Serializable
data class OIDCRequest(
    val client_id: String,
    val client_secret: String,
    val grant_type: String,
    val refresh_token: String,
) {
    companion object {
        fun refreshRequest(clientId: String, clientSecret: String, refreshToken: String): OIDCRequest = OIDCRequest(
            client_id = clientId,
            client_secret = clientSecret,
            grant_type = "refresh_token",
            refresh_token = refreshToken,
        )
    }
}

class SessionRefresher(private val configuration: LibroSession.Configuration) {
    suspend fun refresh(sessionId: String, session: LegacySession): LegacySession {
        val userToken = session.userToken ?: throw Exception("No userToken present")
        val refreshToken = session.refreshToken ?: throw Exception("No refreshToken present")
        val refreshResponse = refreshToken(userToken, refreshToken)
        val newSession = session.copy(
            refreshToken = refreshResponse.refresh_token,
            userToken = refreshResponse.access_token,
        )
        storeSession(sessionId, newSession)

        return newSession
    }

    private suspend fun refreshToken(userToken: String, refreshToken: String): OIDCTokenResponse {
        val client = HttpClient(CIO) {
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
