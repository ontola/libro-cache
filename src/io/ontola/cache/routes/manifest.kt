package io.ontola.cache.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.tenant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Routing.mountManifest() {
    get("*/manifest.json") {
        call.logger.debug { "Requested manifest from external (via handler)" }

        if (call.tenant.websiteIRI.fullPath + "/manifest.json" != call.request.uri) {
            return@get call.respond(HttpStatusCode.NotFound)
        }

        call.respond(Json.encodeToString(call.tenant.manifest))
    }
}
