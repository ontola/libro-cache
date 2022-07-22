package tools.empathy.libro.server.configuration

import io.ktor.http.Url

data class SessionsConfig(
    /**
     * The secret used for verifying session signatures.
     */
    val sessionSecret: String,
    /**
     * Token used to encrypt the session JWTs
     */
    val jwtEncryptionToken: String,
    /**
     * The id to identify this client.
     */
    val clientId: String,
    /**
     * The secret to identify this client.
     */
    val clientSecret: String,
    /**
     * TODO: Refactor away
     */
    val oAuthToken: String,
    /**
     * The url of the OIDC identity provider
     */
    val oidcUrl: Url,
) {
    companion object {
        fun forTesting(): SessionsConfig = SessionsConfig(
            sessionSecret = "secret",
            jwtEncryptionToken = "jwtEncryptionToken",
            clientId = "0",
            clientSecret = "",
            oidcUrl = Url("https://oidcserver.test"),
            oAuthToken = "",
        )
    }
}
