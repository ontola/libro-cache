package io.ontola.cache.sessions

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ontola.cache.plugins.CacheSession
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Deprecated("Until sessions are migrated")
internal suspend fun getLegacySessionOrNull(
    call: ApplicationCall,
    config: CacheSession.Configuration,
): LegacySession? {
    val sessionsConfig = config.cacheConfig.sessions

    val sessionId = getCookieWithInvalidName(call, config.cacheConfig.sessions.cookieNameLegacy)
    val sessionSig = getCookieWithInvalidName(call, config.cacheConfig.sessions.signatureNameLegacy)

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

internal fun splitCookieWithInvalidName(value: String?, cookieName: String): String? = value
    ?.split(";")
    ?.map { it.split("=").map { it.trim() } }
    ?.firstOrNull { it[0] == cookieName }
    ?.last()

internal fun getCookieWithInvalidName(call: ApplicationCall, cookieName: String): String? {
    return splitCookieWithInvalidName(call.request.header("Cookie"), cookieName)
}
