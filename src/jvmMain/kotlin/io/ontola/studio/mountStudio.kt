@file:UseSerializers(UrlSerializer::class)

package io.ontola.studio

import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.persistentStorage
import io.ontola.cache.plugins.sessionManager
import io.ontola.util.UrlSerializer
import io.ontola.util.appendPath
import io.ontola.util.fullUrl
import io.ontola.util.rebase
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Routing.mountStudio() {
    val projectRepo = ProjectRepo(application.persistentStorage)
    val distributionRepo = DistributionRepo(application.persistentStorage)
    val publicationRepo = PublicationRepo(application.persistentStorage)

    val serializer = Json {
        encodeDefaults = true
        isLenient = false
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    get("/_studio/editorContext.bundle.json") {
        call.respond(serializer.encodeToString(EditorContext()))
    }

    post("/_studio/projects") {
        val studioConfig = application.cacheConfig.studio
        if (!studioConfig.skipAuth && !call.sessionManager.isStaff)
            return@post call.respond(HttpStatusCode.Forbidden)

        try {
            val proto = serializer.decodeFromString<ProjectRequest>(call.receive())
            val project = projectRepo.create(proto)
            val body = serializer.encodeToString(
                mapOf(
                    "iri" to studioConfig.origin.appendPath("/_studio/projects/${project.name}").toString(),
                )
            )

            call.respond(body)
        } catch (e: Exception) {
            application.cacheConfig.notify(e)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/_studio/projects") {
        if (!application.cacheConfig.studio.skipAuth && !call.sessionManager.isStaff)
            return@get call.respond(HttpStatusCode.Forbidden)

        val projects = projectRepo
            .findAll()
            .map { call.fullUrl().rebase(it).toString() }

        call.respond(serializer.encodeToString(projects))
    }

    put("/_studio/projects/{projectId}") {
        val studioConfig = application.cacheConfig.studio
        if (!studioConfig.skipAuth && !call.sessionManager.isStaff)
            return@put call.respond(HttpStatusCode.Forbidden)

        val id = call.parameters["projectId"] ?: return@put call.respond(HttpStatusCode.BadRequest)
        try {
            val project = projectRepo.store(id, serializer.decodeFromString(call.receiveText()))
            val body = serializer.encodeToString(
                mapOf(
                    "iri" to studioConfig.origin.appendPath("/_studio/projects/${project.name}").toString(),
                )
            )

            call.respond(body)
        } catch (e: Exception) {
            application.cacheConfig.notify(e)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    get("/_studio/projects/{projectId}") {
        if (!application.cacheConfig.studio.skipAuth && !call.sessionManager.isStaff)
            return@get call.respond(HttpStatusCode.Forbidden)

        val id = call.parameters["projectId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val project = projectRepo.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)

        call.respond(serializer.encodeToString(project))
    }

    get("/_studio/projects/{projectId}/distributions/{distributionId}") {
        if (!application.cacheConfig.studio.skipAuth && !call.sessionManager.isStaff)
            return@get call.respond(HttpStatusCode.Forbidden)

        val projectId = call.parameters["projectId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val distributionId = call.parameters["distributionId"] ?: return@get call.respond(HttpStatusCode.BadRequest)

        val distribution = distributionRepo.get(projectId, distributionId) ?: return@get call.respond(HttpStatusCode.BadRequest)

        call.respond(Json.encodeToString(distribution))
    }

    get("/_studio/projects/{projectId}/distributions/{distributionId}/meta") {
        if (!application.cacheConfig.studio.skipAuth && !call.sessionManager.isStaff)
            return@get call.respond(HttpStatusCode.Forbidden)

        val projectId = call.parameters["projectId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val distributionId = call.parameters["distributionId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val distribution = distributionRepo.get(projectId, distributionId) ?: return@get call.respond(HttpStatusCode.BadRequest)

        call.respond(Json.encodeToString(distribution.meta))
    }

    get("/_studio/projects/{projectId}/distributions") {
        if (!application.cacheConfig.studio.skipAuth && !call.sessionManager.isStaff)
            return@get call.respond(HttpStatusCode.Forbidden)

        val id = call.parameters["projectId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val distributions = distributionRepo.find(id)

        call.respond(serializer.encodeToString(distributions))
    }

    post("/_studio/projects/{projectId}/distributions") {
        if (!application.cacheConfig.studio.skipAuth && !call.sessionManager.isStaff)
            return@post call.respond(HttpStatusCode.Forbidden)

        val id = call.parameters["projectId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val project = projectRepo.get(id) ?: return@post call.respond(HttpStatusCode.BadRequest)
        val meta = Json.decodeFromString<DistributionMeta>(call.receive())

        distributionRepo.store(id, project.toDistribution(meta))

        call.respond(HttpStatusCode.OK)
    }

    post<String>("/_studio/projects/{projectId}/distributions/{distId}/publication") { startRoute ->
        if (!application.cacheConfig.studio.skipAuth && !call.sessionManager.isStaff)
            return@post call.respond(HttpStatusCode.Forbidden)

        val projectId = call.parameters["projectId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val distributionId = call.parameters["distId"] ?: return@post call.respond(HttpStatusCode.BadRequest)

        publicationRepo.store(
            Publication(
                startRoute = Url(startRoute),
                projectId = projectId,
                distributionId = distributionId,
            )
        )

        call.respond(HttpStatusCode.OK)
    }

    post<String>("/_studio/projects/{projectId}/distributions/{distId}/publication/unmount") { startRoute ->
        if (!application.cacheConfig.studio.skipAuth && !call.sessionManager.isStaff)
            return@post call.respond(HttpStatusCode.Forbidden)

        val projectId = call.parameters["projectId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
        val distributionId = call.parameters["distId"] ?: return@post call.respond(HttpStatusCode.BadRequest)

        val publicationWasDeleted = publicationRepo.delete(
            Publication(
                startRoute = Url(startRoute),
                projectId = projectId,
                distributionId = distributionId,
            )
        )

        if (!publicationWasDeleted) return@post call.respond(HttpStatusCode.BadRequest)

        call.respond(HttpStatusCode.OK)
    }

    get("/_studio/projects/{projectId}/publications") {
        if (!application.cacheConfig.studio.skipAuth && !call.sessionManager.isStaff)
            return@get call.respond(HttpStatusCode.Forbidden)

        val projectId = call.parameters["projectId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val publications = publicationRepo.getPublicationsOfProject(projectId)

        call.respond(Json.encodeToString(publications))
    }
}
