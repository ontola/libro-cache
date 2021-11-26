package io.ontola.cache.health

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.client.features.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.services

class BackendCheck : Check() {
    init {
        name = "Backend connectivity"
    }

    override suspend fun runTest(context: PipelineContext<Unit, ApplicationCall>): Exception? {
        context.application.cacheConfig.client.get<HttpResponse>(context.call.services.route("/_public/spi/tenants")) {
            expectSuccess = true
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }

        return null
    }
}
