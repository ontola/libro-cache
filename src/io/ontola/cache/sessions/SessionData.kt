package io.ontola.cache.sessions

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
data class SessionData(
    val accessToken: String,
    val refreshToken: String,
) {
    fun accessTokenBearer(): String = "Bearer $accessToken"

    fun refreshTokenBearer(): String = "Bearer $refreshToken"

    fun claims(jwtValidator: JWTVerifier): Claims? {
        val jwt = jwtValidator.verify(accessToken)
        return json.decodeFromString(Base64().decode(jwt.payload).decodeToString())
    }

    fun isExpired(jwtValidator: com.auth0.jwt.interfaces.JWTVerifier): Boolean {
        try {
            jwtValidator.verify(accessToken)
        } catch (e: TokenExpiredException) {
            return true
        }

        return false
    }
}
