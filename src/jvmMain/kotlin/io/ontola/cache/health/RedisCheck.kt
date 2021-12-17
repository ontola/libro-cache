package io.ontola.cache.health

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.storage

class RedisCheck : Check() {
    init {
        name = "Redis connectivity"
    }

    override suspend fun runTest(context: PipelineContext<Unit, ApplicationCall>): Exception? {
        context.application.storage.getString()
        return null
    }
}
