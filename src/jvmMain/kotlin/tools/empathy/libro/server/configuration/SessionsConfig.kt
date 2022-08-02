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
     * The name to identify this client.
     */
    val clientName: String,
    /**
     * The url of the OIDC identity provider
     */
    val oidcUrl: Url,
) {
    companion object {
        fun forTesting(): SessionsConfig = SessionsConfig(
            sessionSecret = "secret",
            jwtEncryptionToken = "jwtEncryptionToken",
            clientName = "Libro",
            oidcUrl = Url("https://oidcserver.test"),
        )
    }
}
