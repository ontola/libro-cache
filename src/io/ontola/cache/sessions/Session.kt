package io.ontola.cache.sessions

import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.fullPath
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.ontola.cache.plugins.LibroSession
import io.ontola.cache.plugins.TenantData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import mu.KotlinLogging

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class UserData(
    val type: String,
    @SerialName("@id")
    val iri: String,
    val id: String,
    val email: String? = null,
    val language: String,
) {
    @OptIn(ExperimentalSerializationApi::class)
    val asMap: Map<String, Any> by lazy { Properties.encodeToMap(this) }
}

@Serializable
data class Claims(
    @SerialName("application_id")
    val applicationId: String,
    val exp: Long,
    val iat: Long,
    val iss: String? = null,
    @SerialName("profile_id")
    val profileId: String? = null,
    val scopes: List<String>,
    @SerialName("session_id")
    val sessionId: String? = null,
    val user: UserData,
)

@Serializable
data class SessionData(
    val accessToken: String,
    val refreshToken: String,
)

class Session(
    private val configuration: LibroSession.Configuration,
    private val refresher: SessionRefresher,
    private val tenantData: TenantData,
    private val sessionId: String? = null,
    private val sessionSig: String? = null,
    private var session: LegacySession? = null,
    private var lang: String? = null,
    private var host: String? = null,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun ensure() {
        if (session != null) {
            return
        }

        val guestToken = guestToken()
        session = LegacySession(guestToken.accessToken, guestToken.refreshToken)
    }

    fun setAuthorization(authorization: String, refreshToken: String?) {
        session = LegacySession(authorization, refreshToken ?: session?.refreshToken)
    }

    /**
     * Retrieves session information from redis referenced by a libro koa server cookie.
     */
    @OptIn(ExperimentalLettuceCoroutinesApi::class, ExperimentalSerializationApi::class)
    suspend fun legacySession(): LegacySession? {
        if (session != null) {
            return session
        }

        if (sessionId == null) {
            return guestToken().let {
                LegacySession(it.accessToken, it.refreshToken, _expire = it.expiresIn)
            }
        }

        if (!verifySignature(configuration.cookieNameLegacy, configuration.sessionSecret, sessionId, sessionSig)) {
            configuration.cacheConfig.notify(Exception("Invalid sessions signature"))
            return LegacySession()
        }

        session = configuration.adapter.get(sessionId)?.let { json.decodeFromString(it) }

        if (session == null) {
            logger.warn("Session not found")
        } else if (session!!.isExpired(configuration.jwtValidator)) {
            logger.debug("Refresh session")
            session = refresher.refresh(sessionId, session!!)
        }

        return session
    }

    private suspend fun guestToken(): OIDCTokenResponse {
        val client = HttpClient(CIO) {
            install(JsonFeature)
        }

        val accessToken = configuration.accessToken()
        val path = "${tenantData.websiteIRI.fullPath}/oauth/token"

        return client.request("${configuration.oidcUrl}$path") {
            method = HttpMethod.Post
            headers {
                header("Accept", "application/json")
                header("Authorization", "Bearer $accessToken")
                header("Content-Type", "application/json")
                header("Accept-Language", lang)
                header("X-Forwarded-Host", host)
                header("X-Forwarded-Proto", "https")
                header("X-Forwarded-Ssl", "on")
            }

            body = OIDCRequest.guestRequest(configuration.oidcClientId, configuration.oidcClientSecret)
        }
    }

    suspend fun language(): String = claimsFromJWT()?.user?.language
        ?: configuration.cacheConfig.defaultLanguage

    private suspend fun claimsFromJWT(): Claims? = try {
        legacySession()?.claims(configuration.jwtValidator)
    } catch (e: JWTVerificationException) {
        configuration.cacheConfig.notify(e)
        null
    }
}
