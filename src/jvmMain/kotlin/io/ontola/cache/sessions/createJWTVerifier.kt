package io.ontola.cache.sessions

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm

fun createJWTVerifier(jwtEncryptionToken: String, clientId: String): JWTVerifier = JWT
    .require(Algorithm.HMAC512(jwtEncryptionToken))
    .withClaim("application_id", clientId)
    .build()

fun createJWT(jwtEncryptionToken: String, clientId: String, claims: JWTCreator.Builder.() -> Unit): String = JWT
    .create()
    .withClaim("application_id", clientId)
    .apply(claims)
    .sign(Algorithm.HMAC512(jwtEncryptionToken))
