package tools.empathy.libro.server.util

import io.ktor.server.application.ApplicationCall
import tools.empathy.libro.server.plugins.logger
import tools.empathy.libro.server.plugins.requestTimings

/**
 * Measures how long the block will run and adds it to the request timings.
 * @param name The name to use in the timings. Spaces will be converted to underscores.
 */
suspend fun <T : Any?> ApplicationCall.measured(vararg name: String, block: suspend () -> T): T {
    var res: T
    val time = kotlin.system.measureTimeMillis {
        res = block()
    }
    requestTimings.add(name.toList() to time)

    return res
}

suspend fun <T : Any?> ApplicationCall.measuredHit(vararg name: String, block: suspend () -> T, onMissed: suspend () -> T): T {
    var res: T? = null
    var exception: Exception? = null
    var time = 0L
    var missed = false
    time += kotlin.system.measureTimeMillis {
        try {
            res = block()
        } catch (e: Exception) {
            exception = e
        }
    }
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
