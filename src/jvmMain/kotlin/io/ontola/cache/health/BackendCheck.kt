package io.ontola.cache.health

import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.services

class BackendCheck : Check() {
    init {
        name = "Backend connectivity"
    }

    override suspend fun runTest(context: PipelineContext<Unit, ApplicationCall>): Exception? {
        context.application.cacheConfig.client.get(context.call.services.route("/_public/spi/tenants")) {
            expectSuccess = true
        }

        return null
    }
}
