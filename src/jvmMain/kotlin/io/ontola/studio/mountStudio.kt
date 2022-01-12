@file:UseSerializers(UrlSerializer::class)

package io.ontola.studio

import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ontola.cache.plugins.persistentStorage
import io.ontola.cache.plugins.sessionManager
import io.ontola.util.UrlSerializer
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Routing.mountStudio() {
    val projectRepo = ProjectRepo(application.persistentStorage)
    val distributionRepo = DistributionRepo(application.persistentStorage)

    get("/d/studio/editorContext.bundle.json") {
        call.respond(Json.encodeToString(EditorContext()))
    }

    get("/d/studio/docs/:projectId") {
        if (!call.sessionManager.isStaff)
            return@get call.respond(HttpStatusCode.Forbidden)

        val id = call.parameters["projectId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val document = projectRepo.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)

        call.respond(document)
    }

    get("/d/studio/docs/:projectId/distributions") {
        if (!call.sessionManager.isStaff)
            return@get call.respond(HttpStatusCode.Forbidden)

        val id = call.parameters["projectId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val distributions = distributionRepo.find(id)

        call.respond(distributions)
    }

    put<Distribution>("/d/studio/docs/:projectId/distributions") { distribution ->
        if (!call.sessionManager.isStaff)
            return@put call.respond(HttpStatusCode.Forbidden)

        val id = call.parameters["projectId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        distributionRepo.store(id, distribution)

        call.respond(HttpStatusCode.OK)
    }

    post<Url>("/d/studio/docs/:projectId/distributions/:distId/publish") { startRoute ->
        if (!call.sessionManager.isStaff)
            return@post call.respond(HttpStatusCode.Forbidden)

        val projectId = call.parameters["projectId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val distId = call.parameters["distId"] ?: return@post call.respond(HttpStatusCode.BadRequest)

        distributionRepo.publishDistributionToRoute(projectId, distId, startRoute.toString())
    }
}
