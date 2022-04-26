package io.ontola.cache.plugins

import io.ktor.http.HttpMethod
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ontola.cache.sessions.SessionData
import io.ontola.cache.util.CacheHttpHeaders
import org.apache.commons.codec.binary.Base64
import java.security.SecureRandom

private val csrfTokenGenerator = SecureRandom()
private const val csrfTokenSize = 16

class CSRFVerificationException : Exception()

fun generateCSRFToken(): String {
    val bytes = ByteArray(csrfTokenSize)
    csrfTokenGenerator.nextBytes(bytes)

    return Base64().encodeToString(bytes)
}

class CSRFConfiguration {
    var blackList: List<Regex> = emptyList()
    var unsafeMethods = arrayOf(
        HttpMethod.Post,
        HttpMethod.Put,
        HttpMethod.Patch,
        HttpMethod.Delete,
    )

    fun isBlacklisted(method: HttpMethod, path: String): Boolean {
        return unsafeMethods.contains(method) && blackList.none { pattern -> pattern.containsMatchIn(path) }
    }
}

val CsrfProtection = createApplicationPlugin(
    "CsrfProtection",
    ::CSRFConfiguration,
) {
    onCall { call ->
        if (pluginConfig.isBlacklisted(call.request.httpMethod, call.request.path())) {
            val csrfToken = call.sessions.get<SessionData>()?.csrfToken
            val requestToken = call.request.header(CacheHttpHeaders.XCsrfToken)
            val isInvalid = csrfToken == null ||
                Base64().decode(csrfToken).size != csrfTokenSize ||
                csrfToken != requestToken

            if (isInvalid) {
                throw CSRFVerificationException()
            }
        }
    }
}
