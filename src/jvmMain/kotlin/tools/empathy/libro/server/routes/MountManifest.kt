package tools.empathy.libro.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.plugins.logger
import tools.empathy.libro.server.tenantization.tenant
import tools.empathy.url.appendPath

val manifestSerializer = Json {
    encodeDefaults = true
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleManifest() {
    if (call.attributes.contains(AttributeKey<Unit>("StatusPagesTriggered"))) {
        return
    }

    call.logger.debug { "Requested manifest from external (via handler)" }

    if (call.tenant.websiteIRI.appendPath("manifest.json").fullPath != call.request.uri) {
        return call.respond(HttpStatusCode.NotFound)
    }

    call.respond(manifestSerializer.encodeToString(call.tenant.manifest))
}

fun Routing.mountManifest() {
    get("/manifest.json") { handleManifest() }
    get("/*/manifest.json") { handleManifest() }
}
