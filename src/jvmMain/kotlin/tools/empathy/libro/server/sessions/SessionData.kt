package tools.empathy.libro.server.sessions

import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.interfaces.JWTVerifier
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import org.apache.commons.codec.binary.Base64
import tools.empathy.libro.server.plugins.generateCSRFToken

private val json = Json { ignoreUnknownKeys = true }

@Serializable
enum class UserType {
    @SerialName("guest")
    Guest,
    @SerialName("user")
    User,
}

@Serializable
data class UserData(
    val type: UserType,
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
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class SessionData(
    val credentials: TokenPair? = null,
    val deviceId: String? = null,
    @SerialName("csrfToken")
    internal var _csrfToken: String? = generateCSRFToken()
) {
    val csrfToken: String
        get() {
            if (_csrfToken == null) {
                _csrfToken = generateCSRFToken()
            }

            return _csrfToken!!
        }

    fun accessTokenBearer(): String? = if (credentials != null) "Bearer ${credentials.accessToken}" else null

    fun claims(jwtValidator: JWTVerifier): Claims? {
        credentials ?: return null

        val jwt = jwtValidator.verify(credentials.accessToken)
        return json.decodeFromString(Base64().decode(jwt.payload).decodeToString())
    }

    fun isExpired(jwtValidator: JWTVerifier): Boolean {
        try {
            jwtValidator.verify(credentials!!.accessToken)
        } catch (e: TokenExpiredException) {
            return true
        }

        return false
    }
}
