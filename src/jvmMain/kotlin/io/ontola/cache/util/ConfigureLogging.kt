package io.ontola.cache.util

import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.features.CallLogging
import mu.KotlinLogging
import org.slf4j.event.Level

private val kLogger = KotlinLogging.logger {}

fun CallLogging.Configuration.configureCallLogging() {
    level = when {
        kLogger.isTraceEnabled -> Level.TRACE
        kLogger.isDebugEnabled -> Level.DEBUG
        kLogger.isInfoEnabled -> Level.INFO
        kLogger.isWarnEnabled -> Level.WARN
        kLogger.isErrorEnabled -> Level.WARN
        else -> Level.INFO
    }
}

fun Logging.Config.configureClientLogging() {
    level = LogLevel.NONE

    if (kLogger.isTraceEnabled) {
        level = LogLevel.ALL
    } else if (kLogger.isDebugEnabled) {
        level = LogLevel.HEADERS
    }
}
