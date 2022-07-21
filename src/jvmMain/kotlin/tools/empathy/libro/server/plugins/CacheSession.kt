package tools.empathy.libro.server.plugins

import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.client.HttpClient
import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.util.AttributeKey
import tools.empathy.libro.server.sessions.SessionManager

class CacheSessionConfiguration {
    lateinit var client: HttpClient
    lateinit var sessionSecret: String
    lateinit var jwtValidator: JWTVerifier
    lateinit var cacheConfig: CacheConfig
    lateinit var oidcClientId: String
    lateinit var oidcClientSecret: String
    lateinit var oAuthToken: String
    lateinit var oidcUrl: Url

    fun complete(cacheConfig: CacheConfig) {
        if (!this::cacheConfig.isInitialized) this.cacheConfig = cacheConfig
        if (!this::sessionSecret.isInitialized) sessionSecret = cacheConfig.sessions.sessionSecret
        if (!this::client.isInitialized) client = cacheConfig.client
        if (!this::oidcClientId.isInitialized) oidcClientId = cacheConfig.sessions.clientId
        if (!this::oidcClientSecret.isInitialized) oidcClientSecret = cacheConfig.sessions.clientSecret
        if (!this::oAuthToken.isInitialized) oAuthToken = cacheConfig.sessions.oAuthToken
        if (!this::oidcUrl.isInitialized) oidcUrl = cacheConfig.sessions.oidcUrl
    }
}

val CacheSession = createApplicationPlugin(name = "CacheSession", ::CacheSessionConfiguration) {
    pluginConfig.complete(application.cacheConfig)

    onCall { call ->
        call.attributes.put(CacheSessionKey, SessionManager(call, pluginConfig))
    }
}

private val CacheSessionKey = AttributeKey<SessionManager>("CacheSessionKey")

internal val ApplicationCall.sessionManager: SessionManager
    get() = attributes.getOrNull(CacheSessionKey) ?: reportMissingSession()

private fun ApplicationCall.reportMissingSession(): Nothing {
    application.plugin(CacheSession) // ensure the feature is installed
    throw CacheSessionNotYetConfiguredException()
}
class CacheSessionNotYetConfiguredException :
    IllegalStateException("Cache sessions are not yet ready: you are asking it to early before the CacheSessions feature.")
