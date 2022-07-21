package tools.empathy.libro.server.health

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import tools.empathy.libro.server.tenantization.getManifest
import tools.empathy.libro.server.tenantization.getTenants
import tools.empathy.libro.webmanifest.Manifest

class ManifestCheck : Check() {
    init {
        name = "Web manifest"
    }

    override suspend fun runTest(call: ApplicationCall): Exception? {
        val tenant = call.getTenants().sites.first().location
        try {
            call.getManifest<Manifest>(tenant)
        } catch (e: ResponseException) {
            if (e.response.status == HttpStatusCode.Forbidden) {
                return Exception("Backend token invalid")
            }

            return Exception("Expected manifest status 200, was ${e.response.status}")
        }

        return null
    }
}
