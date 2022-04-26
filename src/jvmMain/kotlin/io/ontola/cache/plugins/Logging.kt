package io.ontola.cache.plugins

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.response.header
import io.ktor.util.AttributeKey
import mu.KLogger
import mu.KotlinLogging
import java.time.Instant

private val key = AttributeKey<KLogger>("KLogger")

val Logging = createApplicationPlugin(name = "Logging") {
    val feature = KotlinLogging.logger {}
    feature.info {
        "Running on java ${System.getProperty("java.runtime.version")} ${System.getProperty("java.runtime.name")}"
    }
    application.attributes.put(KLoggerKey, feature)

    onCall { call ->
        call.attributes.put(KLoggerKey, feature)
        call.attributes.put(TimingsKey, mutableListOf(Pair(listOf("tot"), Instant.now().toEpochMilli())))
        call.attributes.put(StartKey, System.currentTimeMillis())
    }

    on(ResponseSent) { call ->
        val start = call.attributes[StartKey]
        val time = System.currentTimeMillis() - start
        call.attributes[TimingsKey][0] = Pair(listOf("tot"), time)
        call.attributes[TimingsKey][0] = Pair(listOf("tot"), Instant.now().toEpochMilli() - call.attributes[TimingsKey][0].second)
        call.response.header(
            "Server-Timing",
            call.attributes[TimingsKey].joinToString(", ") { "${it.first.joinToString(";")};dur=${it.second}" },
        )
    }
}

private val KLoggerKey = AttributeKey<KLogger>("KLoggerKey")

private val TimingsKey = AttributeKey<MutableList<Pair<List<String>, Long>>>("TimingsKey")
private val StartKey = AttributeKey<MutableList<Pair<List<String>, Long>>>("TimingsStartKey")

internal val ApplicationCall.logger: KLogger
    get() = attributes.getOrNull(KLoggerKey) ?: reportMissingRegistry()

internal val ApplicationCall.requestTimings: MutableList<Pair<List<String>, Long>>
    get() = attributes.getOrNull(TimingsKey) ?: throw IllegalStateException("No timing stored in request")

private fun reportMissingRegistry(): Nothing {
    throw LoggingNotYetConfiguredException()
}
class LoggingNotYetConfiguredException :
    IllegalStateException("Logger is not yet ready: you are asking it to early before the Logging feature.")
