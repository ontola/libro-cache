package io.ontola.cache.util

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.requestTimings
import mu.KotlinLogging

private val kLogger = KotlinLogging.logger {}

suspend fun <T : Any> PipelineContext<Unit, ApplicationCall>.measured(name: String, block: suspend () -> T): T {
    lateinit var res: T
    val time = kotlin.system.measureTimeMillis {
        res = block()
    }
    this.call.requestTimings.add(name to time)

    return res
}
