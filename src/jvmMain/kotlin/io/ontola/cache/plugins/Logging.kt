package io.ontola.cache.plugins

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.response.ApplicationSendPipeline
import io.ktor.response.header
import io.ktor.util.AttributeKey
import mu.KLogger
import mu.KotlinLogging
import java.time.Instant
import kotlin.system.measureTimeMillis

/**
 * Application-broad feature.
 */
class Logging {
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Unit, KLogger> {
        override val key = AttributeKey<KLogger>("KLogger")

        override fun install(pipeline: ApplicationCallPipeline, configure: Unit.() -> Unit): KLogger {
            val feature = KotlinLogging.logger {}
            feature.info {
                "Running on java ${System.getProperty("java.runtime.version")} ${System.getProperty("java.runtime.name")}"
            }
            pipeline.attributes.put(KLoggerKey, feature)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                call.attributes.put(KLoggerKey, feature)
                call.attributes.put(TimingsKey, mutableListOf(Pair("tot", Instant.now().toEpochMilli())))
                val time = measureTimeMillis {
                    proceed()
                }
                this.call.attributes[TimingsKey][0] = Pair("tot", time)
            }
            pipeline.sendPipeline.intercept(ApplicationSendPipeline.After) {
                call.attributes[TimingsKey][0] = Pair("tot", Instant.now().toEpochMilli() - call.attributes[TimingsKey][0].second)
                call.response.header(
                    "Server-Timing",
                    call.attributes[TimingsKey].joinToString(", ") { "${it.first};dur=${it.second}" },
                )
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
