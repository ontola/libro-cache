package io.ontola.cache.health

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.routes.headRequest
import io.ontola.cache.tenantization.getTenants

class HeadRequestCheck : Check() {
    init {
        name = "Backend data fetching"
    }

    override suspend fun runTest(call: ApplicationCall): Exception? {
        val tenant = call.getTenants().sites.first().location

        val response = call.headRequest(
            call.application.cacheConfig.client,
            tenant.encodedPath,
            tenant
        )

        if (response.statusCode.value >= HttpStatusCode.BadRequest.value) {
            return Exception("Unexpected status '${response.statusCode}' for 'HEAD /argu'.")
        }

        return null
    }
}
