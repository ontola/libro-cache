package io.ontola

import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.request.*
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands

fun getCookieWithInvalidName(call: ApplicationCall, cookieName: String): String? {
    return call.request
        .header("Cookie")
        ?.split(";")
        ?.map { it.split("=").map { it.trim() } }
        ?.first { it[0] == cookieName }
        ?.last()
}

class LibroSession(private val configuration: Configuration) {
    class Configuration {
        @OptIn(ExperimentalLettuceCoroutinesApi::class)
        lateinit var libroRedisConn: RedisCoroutinesCommands<String, String>
        lateinit var sessionSecret: String
        lateinit var signatureNameLegacy: String
        lateinit var cookieNameLegacy: String
        lateinit var jwtValidator: JWTVerifier
    }

    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        val sessionId = getCookieWithInvalidName(context.call, configuration.cookieNameLegacy)
        val sessionSig = getCookieWithInvalidName(context.call, configuration.signatureNameLegacy)

        // Perform things in that interception point.
        val sessionData = Session(
            configuration = configuration,
            sessionId = sessionId,
            sessionSig = sessionSig
        )
        context.call.attributes.put(LibroSessionKey, sessionData)
    }

    // Implements ApplicationFeature as a companion object.
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, LibroSession> {
        // Creates a unique key for the feature.
        override val key = AttributeKey<LibroSession>("LibroSession")

        // Code to execute when installing the feature.
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): LibroSession {
            val configuration = Configuration().apply(configure)
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
