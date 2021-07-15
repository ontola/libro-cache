package io.ontola.cache.features

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.util.AttributeKey
import mu.KLogger
import mu.KotlinLogging
import kotlin.system.measureTimeMillis

/**
 * Application-broad feature.
 */
class Logging {
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Unit, KLogger> {
        override val key = AttributeKey<KLogger>("KLogger")

        override fun install(pipeline: ApplicationCallPipeline, configure: Unit.() -> Unit): KLogger {
            val feature = KotlinLogging.logger {}
            pipeline.attributes.put(KLoggerKey, feature)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                this.call.attributes.put(KLoggerKey, feature)
                this.call.attributes.put(TimingsKey, mutableListOf(Pair("tot", 0)))
                val time = measureTimeMillis {
                    this.proceed()
                }
                this.call.attributes[TimingsKey][0] = Pair("tot", time)
            }

            return feature
        }
    }
}

private val KLoggerKey = AttributeKey<KLogger>("KLoggerKey")

private val TimingsKey = AttributeKey<MutableList<Pair<String, Long>>>("TimingsKey")

internal val ApplicationCallPipeline.logger: KLogger
    get() = attributes.getOrNull(KLoggerKey) ?: reportMissingRegistry()

internal val ApplicationCall.logger: KLogger
    get() = application.logger

internal val ApplicationCall.requestTimings: MutableList<Pair<String, Long>>
    get() = attributes.getOrNull(TimingsKey) ?: throw IllegalStateException("No timing stored in request")

private fun reportMissingRegistry(): Nothing {
    throw LoggingNotYetConfiguredException()
}
class LoggingNotYetConfiguredException :
    IllegalStateException("Logger is not yet ready: you are asking it to early before the Logging feature.")
