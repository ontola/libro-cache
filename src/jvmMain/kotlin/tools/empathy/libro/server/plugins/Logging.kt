package tools.empathy.libro.server.plugins

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseRouteScopedPlugin
import io.ktor.server.application.call
import io.ktor.server.response.ApplicationSendPipeline
import io.ktor.server.response.header
import io.ktor.util.AttributeKey
import mu.KLogger
import mu.KotlinLogging
import java.time.Instant
import kotlin.system.measureTimeMillis

/**
 * Application-broad feature.
 */
class Logging {
    companion object Plugin : BaseRouteScopedPlugin<Unit, KLogger> {
        override val key = AttributeKey<KLogger>("KLogger")

        override fun install(pipeline: ApplicationCallPipeline, configure: Unit.() -> Unit): KLogger {
            val feature = KotlinLogging.logger {}
            feature.info {
                "Running on java ${System.getProperty("java.runtime.version")} ${System.getProperty("java.runtime.name")}"
            }
            pipeline.attributes.put(KLoggerKey, feature)

            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                call.attributes.put(KLoggerKey, feature)
                call.attributes.put(TimingsKey, mutableListOf(Pair(listOf("tot"), Instant.now().toEpochMilli())))
                val time = measureTimeMillis {
                    proceed()
                }
                call.attributes[TimingsKey][0] = Pair(listOf("tot"), time)
            }
            pipeline.sendPipeline.intercept(ApplicationSendPipeline.After) {
                call.attributes[TimingsKey][0] = Pair(listOf("tot"), Instant.now().toEpochMilli() - call.attributes[TimingsKey][0].second)
                call.response.header(
                    "Server-Timing",
                    call.attributes[TimingsKey].joinToString(", ") { "${it.first.joinToString(";")};dur=${it.second}" },
                )
            }

            return feature
        }
    }
}

private val KLoggerKey = AttributeKey<KLogger>("KLoggerKey")

private val TimingsKey = AttributeKey<MutableList<Pair<List<String>, Long>>>("TimingsKey")

internal val ApplicationCallPipeline.logger: KLogger
    get() = this.attributes.getOrNull(KLoggerKey) ?: reportMissingRegistry()

internal val ApplicationCall.logger: KLogger
    get() = application.logger

internal val ApplicationCall.requestTimings: MutableList<Pair<List<String>, Long>>
    get() = attributes.getOrNull(TimingsKey) ?: throw IllegalStateException("No timing stored in request")

private fun reportMissingRegistry(): Nothing {
    throw LoggingNotYetConfiguredException()
}
class LoggingNotYetConfiguredException :
    IllegalStateException("Logger is not yet ready: you are asking it to early before the Logging feature.")
