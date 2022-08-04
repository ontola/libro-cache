package tools.empathy.libro.server.sessions

import com.auth0.jwt.JWT
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.codec.binary.Base64

sealed class OIDCSession {
    open val claims: Claims? = null

    val isStaff: Boolean
        get() = claims?.scopes?.contains("staff") ?: false
}

object EmptySession : OIDCSession()

class PreAuthSession(val redirect: String?) : OIDCSession()

data class Session(
    val accessToken: String,
    val tokenType: String,
    val refreshToken: String?,
) : OIDCSession() {
    override val claims: Claims
        get() = Json.decodeFromString(Base64().decode(JWT.decode(accessToken).payload).decodeToString())
}
