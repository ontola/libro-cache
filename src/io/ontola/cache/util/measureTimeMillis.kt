package io.ontola.cache.util

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.requestTimings

suspend fun <T : Any?> PipelineContext<*, ApplicationCall>.measured(name: String, block: suspend () -> T): T {
    var res: T
    val time = kotlin.system.measureTimeMillis {
        res = block()
    }
    this.call.requestTimings.add(name to time)

    return res
}

suspend fun <T : Any?> PipelineContext<*, ApplicationCall>.measuredHit(name: String, block: suspend () -> T, missed: suspend () -> T): T {
    var res: T
    var time = 0L
    var missed = false
    time += kotlin.system.measureTimeMillis {
        res = block()
    }
    if (res == null) {
        missed = true
        time += kotlin.system.measureTimeMillis {
            res = missed()
        }
    }
    val postfix = if (missed) "miss" else "hit"
    this.call.requestTimings.add("$name;$postfix" to time)

    return res
}
