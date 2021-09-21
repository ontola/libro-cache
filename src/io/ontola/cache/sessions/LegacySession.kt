package io.ontola.cache.sessions

import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.interfaces.JWTVerifier
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.codec.binary.Base64

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class LegacySession(
    val userToken: String? = null,
    val refreshToken: String? = null,
    val secret: String? = null,
    val count: Long = 0,
    val _expire: Long = 0,
    val _maxAge: Long = 0,
) {
    fun isExpired(jwtValidator: JWTVerifier): Boolean {
        val userToken = userToken ?: return false

        try {
            jwtValidator.verify(userToken)
        } catch (e: TokenExpiredException) {
            return true
        }

        return false
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun claims(jwtValidator: JWTVerifier): Claims? {
        if (userToken == null) return null

        val jwt = jwtValidator.verify(userToken)
        return json.decodeFromString(Base64().decode(jwt.payload).decodeToString())
    }
}
