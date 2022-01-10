package io.ontola.cache.plugins

import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.client.HttpClient
import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.application.plugin
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.sessions.SessionManager

class CacheSession(private val configuration: Configuration) {
    class Configuration {
        @Deprecated("Until sessions are migrated")
        lateinit var legacyStorageAdapter: StorageAdapter<String, String>
        lateinit var client: HttpClient
        lateinit var sessionSecret: String
        lateinit var jwtValidator: JWTVerifier
        lateinit var cacheConfig: CacheConfig
        lateinit var oidcClientId: String
        lateinit var oidcClientSecret: String
        lateinit var oAuthToken: String
        lateinit var oidcUrl: Url
    }

    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        context.call.attributes.put(CacheSessionKey, SessionManager(context.call, configuration))
    }

    companion object Plugin : ApplicationPlugin<ApplicationCallPipeline, Configuration, CacheSession> {
        override val key = AttributeKey<CacheSession>("CacheSession")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): CacheSession {
            val cacheConfig = pipeline.cacheConfig
            val configuration = Configuration()
                .apply {
                    this.sessionSecret = cacheConfig.sessions.sessionSecret
                    this.cacheConfig = cacheConfig
                    this.client = cacheConfig.client

                    this.oidcClientId = cacheConfig.sessions.clientId
                    this.oidcClientSecret = cacheConfig.sessions.clientSecret
                    this.oAuthToken = cacheConfig.sessions.oAuthToken
                    this.oidcUrl = cacheConfig.sessions.oidcUrl
                }
                .apply(configure)
            val feature = CacheSession(configuration)

            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                feature.intercept(this)
            }

            return feature
        }
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
