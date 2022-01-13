@file:UseSerializers(UrlSerializer::class)

package io.ontola.studio

import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ontola.cache.plugins.persistentStorage
import io.ontola.cache.plugins.sessionManager
import io.ontola.util.UrlSerializer
import io.ontola.util.fullUrl
import io.ontola.util.rebase
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Routing.mountStudio() {
    val projectRepo = ProjectRepo(application.persistentStorage)
    val distributionRepo = DistributionRepo(application.persistentStorage)

    val serializer = Json {
        encodeDefaults = true
        isLenient = false
        ignoreUnknownKeys = false
        prettyPrint = true
    }

    get("/_studio/editorContext.bundle.json") {
        call.respond(serializer.encodeToString(EditorContext()))
    }

    post("/_studio/projects") {
        if (!call.sessionManager.isStaff)
            return@post call.respond(HttpStatusCode.Forbidden)

        val proto = serializer.decodeFromString<ProjectRequest>(call.receive())
        val project = projectRepo.create(proto)

        call.respond(serializer.encodeToString(mapOf("iri" to "https://local.rdf.studio/_studio/projects/${project.name}")))
    }

    get("/_studio/projects") {
        if (!call.sessionManager.isStaff)
            return@get call.respond(HttpStatusCode.Forbidden)

        val projects = projectRepo
            .findAll()
            .map { call.fullUrl().rebase(it.last()).toString() }

        call.respond(serializer.encodeToString(projects))
    }

    put("/_studio/projects/{projectId}") {
        if (!call.sessionManager.isStaff)
            return@put call.respond(HttpStatusCode.Forbidden)

        val id = call.parameters["projectId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        val project = projectRepo.store(id, serializer.decodeFromString(call.receiveText()))

        call.respond(serializer.encodeToString(mapOf("iri" to "https://local.rdf.studio/_studio/projects/${project.name}")))
    }

    get("/_studio/projects/{projectId}") {
        if (!call.sessionManager.isStaff)
            return@get call.respond(HttpStatusCode.Forbidden)

        val id = call.parameters["projectId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val project = projectRepo.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)

        call.respond(serializer.encodeToString(project))
    }

    get("/_studio/projects/{projectId}/distributions") {
        if (!call.sessionManager.isStaff)
            return@get call.respond(HttpStatusCode.Forbidden)

        val id = call.parameters["projectId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val distributions = distributionRepo.find(id)

        call.respond(distributions)
    }

    put<Distribution>("/_studio/projects/{projectId}/distributions") {
        if (!call.sessionManager.isStaff)
            return@put call.respond(HttpStatusCode.Forbidden)

        val distribution = Json.decodeFromString<Distribution>(call.receive())
        val id = call.parameters["projectId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        distributionRepo.store(id, distribution)

        call.respond(HttpStatusCode.OK)
    }

    post<Url>("/_studio/projects/{projectId}/distributions/{distId}/publish") { startRoute ->
        if (!call.sessionManager.isStaff)
            return@post call.respond(HttpStatusCode.Forbidden)

        val projectId = call.parameters["projectId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val distId = call.parameters["distId"] ?: return@post call.respond(HttpStatusCode.BadRequest)

        distributionRepo.publishDistributionToRoute(projectId, distId, startRoute.toString())
    }
}
