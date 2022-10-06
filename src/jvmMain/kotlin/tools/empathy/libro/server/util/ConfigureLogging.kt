package tools.empathy.libro.server.util

import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.server.plugins.callloging.CallLoggingConfig
import mu.KotlinLogging
import org.slf4j.event.Level

private val kLogger = KotlinLogging.logger {}

fun CallLoggingConfig.configureCallLogging() {
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
        level = LogLevel.INFO
    }
}
