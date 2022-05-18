package io.ontola.cache.util

import io.ktor.server.application.ApplicationCall
import io.ontola.cache.initializeOpenTelemetry
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.requestTimings
import io.opentelemetry.api.trace.Span
import kotlinx.css.time

private val openTelemetry = initializeOpenTelemetry()
private val tracer = openTelemetry.getTracer("io.ontola.cache")

/**
 * Measures how long the block will run and adds it as a span.
 * @param name The name to use in the timings. Spaces will be converted to underscores.
 */
fun <T : Any?> withSpan(vararg name: String, block: (span: Span) -> T): T {
    val res: T
    val span = tracer.spanBuilder(name.joinToString(":")).startSpan()
    res = block(span)
    span.end()

    return res
}

/**
 * Measures how long the block will run and adds it as a span.
 * @param name The name to use in the timings. Spaces will be converted to underscores.
 */
suspend fun <T : Any?> withAsyncSpan(vararg name: String, block: suspend (span: Span) -> T): T {
    val res: T
    val span = tracer.spanBuilder(name.joinToString(":")).startSpan()
    res = block(span)
    span.end()

    return res
}

/**
 * Measures how long the block will run and adds it to the request timings.
 * @param name The name to use in the timings. Spaces will be converted to underscores.
 */
suspend fun <T : Any?> ApplicationCall.measured(vararg name: String, block: suspend (span: Span) -> T): T {
    var res: T
    val span = tracer.spanBuilder(name.joinToString(":")).startSpan()
    val time = kotlin.system.measureTimeMillis {
        res = block(span)
    }
    span.end()
    requestTimings.add(name.toList() to time)

    return res
}

suspend fun <T : Any?> ApplicationCall.measuredHit(vararg name: String, block: suspend () -> T, onMissed: suspend () -> T): T {
    var res: T? = null
    var exception: Exception? = null
    var time = 0L
    var missed = false
    val span = tracer.spanBuilder(name.joinToString(":")).startSpan()
    time += kotlin.system.measureTimeMillis {
        try {
            res = block()
        } catch (e: Exception) {
            exception = e
        }
    }
    span.end()
    if (res == null) {
        missed = true
        time += kotlin.system.measureTimeMillis {
            try {
                res = onMissed()
            } catch (e: Exception) {
                exception = e
            }
        }
    }
    val postfix = if (missed) "miss" else "hit"
    logger.debug {
        if (missed) {
            "[cache] $postfix for ${name.joinToString(":")}"
        } else {
            "[cache] $postfix for ${name.joinToString(":")}"
        }
    }
    val measureName = buildList {
        addAll(name)
        add(postfix)
    }.toList()
    requestTimings.add(measureName to time)

    if (exception != null) {
        throw exception as Exception
    }

    return res as T
}
