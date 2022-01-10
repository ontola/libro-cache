package io.ontola.cache.routes

import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.server.application.call
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.util.AttributeKey
import io.ontola.cache.plugins.logger
import io.ontola.cache.tenantization.tenant
import io.ontola.util.appendPath
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Routing.mountManifest() {
    get("*/manifest.json") {
        if (call.attributes.contains(AttributeKey<Unit>("StatusPagesTriggered"))) {
            return@get
        }

        call.logger.debug { "Requested manifest from external (via handler)" }

        if (call.tenant.websiteIRI.appendPath("manifest.json").fullPath != call.request.uri) {
            return@get call.respond(HttpStatusCode.NotFound)
        }

        call.respond(Json.encodeToString(call.tenant.manifest))
    }
}
