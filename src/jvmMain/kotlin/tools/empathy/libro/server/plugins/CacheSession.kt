package tools.empathy.libro.server.plugins

import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.client.HttpClient
import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.util.AttributeKey
import tools.empathy.libro.server.configuration.LibroConfig
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.sessions.SessionManager

class CacheSessionConfiguration {
    lateinit var client: HttpClient
    lateinit var sessionSecret: String
    lateinit var jwtValidator: JWTVerifier
    lateinit var libroConfig: LibroConfig
    lateinit var oidcClientId: String
    lateinit var oidcClientSecret: String
    lateinit var oidcUrl: Url

    fun complete(libroConfig: LibroConfig) {
        if (!this::libroConfig.isInitialized) this.libroConfig = libroConfig
        if (!this::sessionSecret.isInitialized) sessionSecret = libroConfig.sessions.sessionSecret
        if (!this::client.isInitialized) client = libroConfig.client
        if (!this::oidcClientId.isInitialized) oidcClientId = libroConfig.sessions.clientId
        if (!this::oidcClientSecret.isInitialized) oidcClientSecret = libroConfig.sessions.clientSecret
        if (!this::oidcUrl.isInitialized) oidcUrl = libroConfig.sessions.oidcUrl
    }
}

val CacheSession = createApplicationPlugin(name = "CacheSession", ::CacheSessionConfiguration) {
    pluginConfig.complete(application.libroConfig)

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
