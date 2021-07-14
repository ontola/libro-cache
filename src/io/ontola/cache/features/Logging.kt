package io.ontola.cache.features

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.util.AttributeKey
import io.ktor.util.KtorExperimentalAPI
import mu.KLogger
import mu.KotlinLogging

/**
 * Application-broad feature.
 */
class Logging {
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Unit, KLogger> {
        override val key = AttributeKey<KLogger>("KLogger")

        @KtorExperimentalAPI
        override fun install(pipeline: ApplicationCallPipeline, configure: Unit.() -> Unit): KLogger {
            val feature = KotlinLogging.logger {}
            pipeline.attributes.put(KLoggerKey, feature)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                this.call.attributes.put(KLoggerKey, feature)
            }
            return feature
        }
    }
}

private val KLoggerKey = AttributeKey<KLogger>("KLoggerKey")

internal val ApplicationCallPipeline.logger: KLogger
    get() = attributes.getOrNull(KLoggerKey) ?: reportMissingRegistry()

internal val ApplicationCall.logger: KLogger
    get() = application.logger

private fun reportMissingRegistry(): Nothing {
    throw LoggingNotYetConfiguredException()
}
class LoggingNotYetConfiguredException :
    IllegalStateException("Logger is not yet ready: you are asking it to early before the Logging feature.")
