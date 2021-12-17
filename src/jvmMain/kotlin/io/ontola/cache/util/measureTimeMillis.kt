package io.ontola.cache.util

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.requestTimings

/**
 * Measures how long the block will run and adds it to the request timings.
 * @param name The name to use in the timings. Spaces will be converted to underscores.
 */
suspend fun <T : Any?> PipelineContext<*, ApplicationCall>.measured(name: String, block: suspend () -> T): T {
    var res: T
    val time = kotlin.system.measureTimeMillis {
        res = block()
    }
    call.requestTimings.add(name.replace(' ', '_') to time)

    return res
}

suspend fun <T : Any?> PipelineContext<*, ApplicationCall>.measuredHit(name: String, block: suspend () -> T, onMissed: suspend () -> T): T {
    var res: T? = null
    var exception: Exception? = null
    var time = 0L
    var missed = false
    time += kotlin.system.measureTimeMillis {
        try {
            res = block()
        } catch(e: Exception) {
            exception = e
        }
    }
    if (res == null) {
        missed = true
        time += kotlin.system.measureTimeMillis {
            try {
                res = onMissed()
            } catch(e: Exception) {
                exception = e
            }
        }
    }
    val postfix = if (missed) "miss" else "hit"
    call.logger.debug {
        if (missed) {
            "[cache] $postfix for $name"
        } else {
            "[cache] $postfix for $name"
        }
    }
    call.requestTimings.add("$name;$postfix" to time)

    if (exception != null) {
        throw exception as Exception
    }

    return res as T
}
