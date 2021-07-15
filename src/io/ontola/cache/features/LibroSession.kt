package io.ontola.cache.features

import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.request.header
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.ontola.cache.sessions.Session
import io.ontola.cache.sessions.SessionRefresher

fun getCookieWithInvalidName(call: ApplicationCall, cookieName: String): String? {
    return call.request
        .header("Cookie")
        ?.split(";")
        ?.map { it.split("=").map { it.trim() } }
        ?.firstOrNull { it[0] == cookieName }
        ?.last()
}

class LibroSession(private val configuration: Configuration) {
    class Configuration {
        @OptIn(ExperimentalLettuceCoroutinesApi::class)
        lateinit var adapter: StorageAdapter<String, String>
        lateinit var sessionSecret: String
        lateinit var signatureNameLegacy: String
        lateinit var cookieNameLegacy: String
        lateinit var jwtValidator: JWTVerifier
        lateinit var cacheConfig: CacheConfig
        lateinit var oidcClientId: String
        lateinit var oidcClientSecret: String
        lateinit var oidcUrl: String
    }

    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        val sessionId = getCookieWithInvalidName(context.call, configuration.cookieNameLegacy)
        val sessionSig = getCookieWithInvalidName(context.call, configuration.signatureNameLegacy)
        val refresher = SessionRefresher(configuration)

        context.call.logger.debug {
            sessionId?.let { "sessionId $it" } ?: "No sessionId"
        }

        val sessionData = Session(
            configuration = configuration,
            refresher = refresher,
            sessionId = sessionId,
            sessionSig = sessionSig
        )
        context.call.attributes.put(LibroSessionKey, sessionData)
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, LibroSession> {
        override val key = AttributeKey<LibroSession>("LibroSession")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): LibroSession {
            val cacheConfig = pipeline.cacheConfig
            val configuration = Configuration()
                .apply {
                    this.sessionSecret = cacheConfig.sessions.sessionSecret
                    this.cacheConfig = cacheConfig
                }
                .apply(configure)
            val feature = LibroSession(configuration)
            pipeline.intercept(ApplicationCallPipeline.Features) {
                feature.intercept(this)
            }

            return feature
        }
    }
}

private val LibroSessionKey = AttributeKey<Session>("LibroSessionKey")

internal val ApplicationCall.session: Session
    get() = attributes.getOrNull(LibroSessionKey) ?: reportMissingTenantization()

private fun ApplicationCall.reportMissingTenantization(): Nothing {
    application.feature(LibroSession) // ensure the feature is installed
    throw LibroSessionNotYetConfiguredException()
}
class LibroSessionNotYetConfiguredException :
    IllegalStateException("Libro sessions are not yet ready: you are asking it to early before the LibroSessions feature.")
