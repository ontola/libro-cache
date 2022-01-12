package io.ontola.studio

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ontola.cache.plugins.persistentStorage
import io.ontola.cache.plugins.sessionManager

fun Routing.mountStudio() {
    post<Distribution>("/d/studio/doc/:id/distribution") { distribution ->
        if (!call.sessionManager.isStaff) {
            return@post call.respond(HttpStatusCode.Forbidden)
        }

        val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val documentRepo = DistributionRepo(call.application.persistentStorage)

        documentRepo.store(id, distribution)

        call.respond(HttpStatusCode.OK)
    }
}
