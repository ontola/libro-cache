@file:UseSerializers(UrlSerializer::class)

package tools.empathy.studio

import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.plugins.persistentStorage
import tools.empathy.libro.server.sessions.OIDCSession
import tools.empathy.url.UrlSerializer
import tools.empathy.url.appendPath
import tools.empathy.url.fullUrl
import tools.empathy.url.rebase

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

    fun ApplicationCall.isStudioAllowed(): Boolean =
        application.libroConfig.studio.skipAuth || sessions.get<OIDCSession>()?.isStaff == true

    suspend fun PipelineContext<Unit, ApplicationCall>.onlyIfAllowed(allowed: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit) {
        if (!call.isStudioAllowed()) {
            val status = if (call.sessions.get<OIDCSession>() == null) HttpStatusCode.Unauthorized else HttpStatusCode.Forbidden
            call.respond(status)
        } else {
            allowed()
        }
    }

    get("/_studio/editorContext.bundle.json") {
        call.respond(serializer.encodeToString(EditorContext()))
    }

    post("/_studio/projects") {
        onlyIfAllowed {
            val studioConfig = application.libroConfig.studio

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
                application.libroConfig.notify(e)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }

    get("/_studio/projects") {
        onlyIfAllowed {
            val projects = projectRepo
                .findAll()
                .map { call.fullUrl().rebase(it).toString() }

            call.respond(serializer.encodeToString(projects))
        }
    }

    put("/_studio/projects/{projectId}") {
        onlyIfAllowed {
            val studioConfig = application.libroConfig.studio
            val id = call.parameters["projectId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)
            try {
                val project = projectRepo.store(id, serializer.decodeFromString(call.receiveText()))
                val body = serializer.encodeToString(
                    mapOf(
                        "iri" to studioConfig.origin.appendPath("/_studio/projects/${project.name}").toString(),
                    ),
                )

                call.respond(body)
            } catch (e: Exception) {
                application.libroConfig.notify(e)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }

    get("/_studio/projects/{projectId}") {
        onlyIfAllowed {
            val id = call.parameters["projectId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)
            val project = projectRepo.get(id) ?: return@onlyIfAllowed call.respond(HttpStatusCode.NotFound)

            call.respond(serializer.encodeToString(project))
        }
    }

    get("/_studio/projects/{projectId}/distributions/{distributionId}") {
        onlyIfAllowed {
            val projectId = call.parameters["projectId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)
            val distributionId = call.parameters["distributionId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)

            val distribution = distributionRepo.get(projectId, distributionId) ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)

            call.respond(Json.encodeToString(distribution))
        }
    }

    get("/_studio/projects/{projectId}/distributions/{distributionId}/meta") {
        onlyIfAllowed {
            val projectId = call.parameters["projectId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)
            val distributionId = call.parameters["distributionId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)
            val distribution = distributionRepo.get(projectId, distributionId) ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)

            call.respond(Json.encodeToString(distribution.meta))
        }
    }

    get("/_studio/projects/{projectId}/distributions") {
        onlyIfAllowed {
            val id = call.parameters["projectId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)
            val distributions = distributionRepo.find(id)

            call.respond(serializer.encodeToString(distributions))
        }
    }

    post("/_studio/projects/{projectId}/distributions") {
        onlyIfAllowed {
            val id = call.parameters["projectId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)
            val project = projectRepo.get(id) ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)
            val meta = Json.decodeFromString<DistributionMeta>(call.receive())

            distributionRepo.store(id, project.toDistribution(meta))

            call.respond(HttpStatusCode.OK)
        }
    }

    post<String>("/_studio/projects/{projectId}/distributions/{distId}/publication") { startRoute ->
        onlyIfAllowed {
            val projectId = call.parameters["projectId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)
            val distributionId = call.parameters["distId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)

            publicationRepo.store(
                Publication(
                    startRoute = Url(startRoute),
                    projectId = projectId,
                    distributionId = distributionId,
                )
            )

            call.respond(HttpStatusCode.OK)
        }
    }

    post<String>("/_studio/projects/{projectId}/distributions/{distId}/publication/unmount") { startRoute ->
        onlyIfAllowed {
            val projectId = call.parameters["projectId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)
            val distributionId = call.parameters["distId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)

            val publicationWasDeleted = publicationRepo.delete(
                Publication(
                    startRoute = Url(startRoute),
                    projectId = projectId,
                    distributionId = distributionId,
                )
            )

            if (!publicationWasDeleted) return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)

            call.respond(HttpStatusCode.OK)
        }
    }

    get("/_studio/projects/{projectId}/publications") {
        onlyIfAllowed {
            val projectId = call.parameters["projectId"] ?: return@onlyIfAllowed call.respond(HttpStatusCode.BadRequest)
            val publications = publicationRepo.getPublicationsOfProject(projectId)

            call.respond(Json.encodeToString(publications))
        }
    }
}
