package io.ontola.cache.sessions

import io.ktor.application.ApplicationCall
import io.ontola.cache.plugins.CacheSession
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Deprecated("Until sessions are migrated")
internal suspend fun getLegacySessionOrNull(
    call: ApplicationCall,
    config: CacheSession.Configuration,
): LegacySession? {
    val sessionsConfig = config.cacheConfig.sessions

    val sessionId = call.request.cookies[config.cacheConfig.sessions.cookieNameLegacy]
    val sessionSig = call.request.cookies[config.cacheConfig.sessions.signatureNameLegacy]

    if (sessionId == null || sessionSig == null) {
        return null
    }

    if (!verifySignature(sessionsConfig.cookieNameLegacy, sessionsConfig.sessionSecret, sessionId, sessionSig)) {
        config.cacheConfig.notify(Exception("Invalid sessions signature"))
        return null
    }

    return config
        .legacyStorageAdapter
        .get(sessionId)
        ?.let { Json.decodeFromString<LegacySession>(it) }
        ?.takeUnless { it.isExpired(config.jwtValidator) }
}
