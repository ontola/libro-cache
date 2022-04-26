package io.ontola.cache.health

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.tenantization.getManifest
import io.ontola.cache.tenantization.getTenants

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
