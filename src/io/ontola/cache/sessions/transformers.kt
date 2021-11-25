package io.ontola.cache.sessions

import io.ktor.sessions.SessionTransportTransformer
import io.ktor.sessions.SessionTransportTransformerMessageAuthentication

fun signedTransformer(
    signingSecret: String,
): SessionTransportTransformer {
    return SessionTransportTransformerMessageAuthentication(
        signingSecret.toByteArray(),
        algorithm = "HmacSHA256"
    )
}
