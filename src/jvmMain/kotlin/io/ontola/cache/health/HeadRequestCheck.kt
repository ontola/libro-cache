package io.ontola.cache.health

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.application
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.routes.headRequest
import io.ontola.cache.tenantization.getTenants

class HeadRequestCheck : Check() {
    init {
        name = "Backend data fetching"
    }

    override suspend fun runTest(context: PipelineContext<Unit, ApplicationCall>): Exception? {
        val tenant = context.getTenants().sites.first().location

        val response = context.headRequest(
            context.application.cacheConfig.client,
            tenant.encodedPath,
            tenant
        )

        if (response.statusCode.value >= HttpStatusCode.BadRequest.value) {
            return Exception("Unexpected status '${response.statusCode}' for 'HEAD /argu'.")
        }

        return null
    }
}
