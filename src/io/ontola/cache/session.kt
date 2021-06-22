package io.ontola.cache

import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.util.hex
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.ontola.cache.features.LibroSession
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils

@Serializable
data class UserData(
    val type: String,
    @SerialName("@id")
    val iri: String,
    val id: String,
    val email: String? = null,
    val language: String,
)

@Serializable
data class Claims(
    @SerialName("application_id")
    val applicationId: String,
    val exp: Long,
    val iat: Long,
    val scopes: List<String>,
    val user: UserData,
)

class Session(
    private val configuration: LibroSession.Configuration,
    private val sessionId: String? = null,
    private val sessionSig: String? = null,
    private var session: LegacySession? = null,
    private var language: String? = null,
) {
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

        val checkInput = "${configuration.cookieNameLegacy}=$sessionId".toByteArray()
        val commo = HmacUtils(HmacAlgorithms.HMAC_SHA_1, configuration.sessionSecret).hmacHex(checkInput)
        // https://github.com/tj/node-cookie-signature/blob/master/index.js#L23
        val final = Base64().encodeAsString(hex(commo))
            .replace('/', '_')
            .replace('+', '-')
            .replace("=", "")

        if (sessionSig != final) {
            configuration.cacheConfig.notify(Exception("Invalid session signature"))
            return LegacySession() // TODO: Throw security exception?
        }

        session = configuration.libroRedisConn.get(sessionId)?.let { Json.decodeFromString(it) }

        return session
    }

    suspend fun language(): String? = claimsFromJWT()?.user?.language


    private suspend fun claimsFromJWT(): Claims? {
        val sess = legacySession() ?: return null
        val userToken = sess.userToken ?: return null

        return try {
            val jwt = configuration.jwtValidator.verify(userToken)
            Json.decodeFromString<Claims>(Base64().decode(jwt.payload).decodeToString())
        } catch (e: JWTVerificationException) {
            configuration.cacheConfig.notify(e)
            null
        }
    }
}
