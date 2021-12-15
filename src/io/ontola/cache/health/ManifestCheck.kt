package io.ontola.cache.health

import io.ktor.application.ApplicationCall
import io.ktor.client.features.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.tenantization.Manifest
import io.ontola.cache.tenantization.getManifest
import io.ontola.cache.tenantization.getTenants

class ManifestCheck : Check() {
    init {
        name = "Web manifest"
    }

    override suspend fun runTest(context: PipelineContext<Unit, ApplicationCall>): Exception? {
        val tenant = context.getTenants().sites.first().location
        try {
            context.getManifest<Manifest>(tenant)
        } catch (e: ResponseException) {
            if (e.response.status == HttpStatusCode.Forbidden) {
                return Exception("Backend token invalid")
            }

            return Exception("Expected manifest status 200, was ${e.response.status}");
        }

        return null
    }
}