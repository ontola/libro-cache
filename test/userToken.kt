import io.ontola.cache.sessions.UserData
import io.ontola.cache.sessions.createJWT
import java.time.Instant
import java.util.Date

fun userToken(expired: Boolean = false): String {
    val year = if (expired) "2010" else "2030"

    val userData = UserData(
        type = "GuestUser",
        iri = "https://example.com/user",
        email = "user@example.com",
        id = "5",
        language = "nl",
    )

    return createJWT("12345", "CLIENT_ID") {
        withClaim("profile_id", "PROFILE_ID")
        withClaim("user", userData.asMap)
        withClaim("scopes", listOf("scope1"))
        withExpiresAt(Date.from(Instant.parse("$year-01-01T12:00:00Z")))
        withIssuedAt(Date.from(Instant.EPOCH))
    }
}
