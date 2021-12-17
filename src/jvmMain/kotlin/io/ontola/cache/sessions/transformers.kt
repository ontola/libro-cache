package io.ontola.cache.sessions

import io.ktor.server.sessions.SessionTransportTransformer
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication

fun signedTransformer(
    signingSecret: String,
): SessionTransportTransformer {
    return SessionTransportTransformerMessageAuthentication(
        signingSecret.toByteArray(),
        algorithm = "HmacSHA256"
    )
}
