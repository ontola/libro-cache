package tools.empathy.libro.server.health

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.routes.headRequest
import tools.empathy.libro.server.tenantization.getExternalTenants

class HeadRequestCheck : Check() {
    init {
        name = "Backend data fetching"
    }

    override suspend fun runTest(call: ApplicationCall): Exception? {
        val tenant = call.getExternalTenants().first().location

        val response = call.headRequest(
            call.application.libroConfig.client,
            tenant.encodedPath,
            tenant
        )

        if (response.statusCode.value >= HttpStatusCode.BadRequest.value) {
            return Exception("Unexpected status '${response.statusCode}' for 'HEAD /argu'.")
        }

        return null
    }
}
