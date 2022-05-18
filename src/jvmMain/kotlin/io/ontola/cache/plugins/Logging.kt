package io.ontola.cache.plugins

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.CallSetup
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.response.header
import io.ktor.server.routing.Routing
import io.ktor.util.AttributeKey
import mu.KLogger
import mu.KotlinLogging
import java.time.Instant

val Logging = createApplicationPlugin("Logging") {
    val feature = KotlinLogging.logger {}
    application.attributes.put(KLoggerKey, feature)
    feature.info {
        "Running on java ${System.getProperty("java.runtime.version")} ${System.getProperty("java.runtime.name")}"
    }

    on(CallSetup) { call ->
        call.attributes.put(
            KLoggerKey,
            feature
        )
        call.attributes.put(
            TimingsKey,
            mutableListOf(
                Pair(listOf("tot"), Instant.now().toEpochMilli())
            )
        )
    }

    on(MonitoringEvent(Routing.RoutingCallFinished)) { call ->
        call.attributes[TimingsKey][0] =
            Pair(listOf("tot"), Instant.now().toEpochMilli() - call.attributes[TimingsKey][0].second)
        call.response.header(
            "Server-Timing",
            call.attributes[TimingsKey].joinToString(", ") { "${it.first.joinToString(";")};dur=${it.second}" },
        )
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
