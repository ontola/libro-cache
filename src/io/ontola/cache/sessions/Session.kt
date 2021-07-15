package io.ontola.cache.sessions

import com.auth0.jwt.exceptions.JWTVerificationException
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.ontola.cache.plugins.LibroSession
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import mu.KotlinLogging

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

class Session(
    private val configuration: LibroSession.Configuration,
    private val refresher: SessionRefresher,
    private val sessionId: String? = null,
    private val sessionSig: String? = null,
    private var session: LegacySession? = null,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Retrieves session information from redis referenced by a libro koa server cookie.
     */
    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun legacySession(): LegacySession? {
        if (session != null) {
            return session
        }

        if (sessionId == null) {
            return null
        }

        if (!verifySignature(configuration.cookieNameLegacy, configuration.sessionSecret, sessionId, sessionSig)) {
            configuration.cacheConfig.notify(Exception("Invalid sessions signature"))
            return LegacySession()
        }

        session = configuration.adapter.get(sessionId)?.let { Json.decodeFromString(it) }

        if (session == null) {
            logger.warn("Session not found")
        } else if (session!!.isExpired(configuration.jwtValidator)) {
            logger.debug("Refresh session")
            session = refresher.refresh(sessionId, session!!)
        }

        return session
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
