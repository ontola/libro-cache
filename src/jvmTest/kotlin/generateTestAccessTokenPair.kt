import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ontola.cache.plugins.SessionsConfig
import io.ontola.cache.sessions.UserType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.util.Date
import kotlin.time.Duration.Companion.hours

fun generateTestAccessTokenPair(
    expired: Boolean = false,
    userType: UserType = UserType.User,
): Pair<String, String> {
    val config = SessionsConfig.forTesting()

    val accessToken = JWT
        .create()
        .withClaim("application_id", config.clientId)
        .withIssuer(config.oidcUrl.toString())
        .withIssuedAt(Date.from(Clock.System.now().toJavaInstant()))
        .withExpiresAt(Date.from(Clock.System.now().plus(1.hours).toJavaInstant()))
        .withClaim("scopes", listOf(userType.name.lowercase()))
        .withClaim(
            userType.name.lowercase(),
            mapOf(
                "type" to userType.name.lowercase(),
                "iri" to "",
                "@id" to "",
                "id" to "",
                "language" to "en",
            )
        )
        .withClaim("application_id", config.clientId)
        .apply {
            if (expired) {
                this.withExpiresAt(Date.from(Instant.DISTANT_PAST.toJavaInstant()))
            }
        }
        .sign(Algorithm.HMAC512(config.jwtEncryptionToken))
    val refreshToken = JWT
        .create()
        .withClaim("application_id", config.clientId)
        .sign(Algorithm.HMAC512(config.jwtEncryptionToken))

    return Pair(accessToken, refreshToken)
}
