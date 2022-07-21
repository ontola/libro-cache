package tools.empathy.libro.server.health

import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.server.application.ApplicationCall
import tools.empathy.libro.server.plugins.cacheConfig
import tools.empathy.libro.server.plugins.services

class BackendCheck : Check() {
    init {
        name = "Backend connectivity"
    }

    override suspend fun runTest(call: ApplicationCall): Exception? {
        call.application.cacheConfig.client.get(call.services.route("/_public/spi/tenants")) {
            expectSuccess = true
        }

        return null
    }
}
