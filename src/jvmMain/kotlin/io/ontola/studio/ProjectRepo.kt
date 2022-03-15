package io.ontola.studio

import io.ktor.http.Url
import io.ktor.server.plugins.NotFoundException
import io.ontola.cache.plugins.Storage
import kotlinx.coroutines.FlowPreview
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProjectRepo(val storage: Storage) {
    private val projectSetKey = projectsPart

    private fun projectKey(projectId: String): Array<String> = arrayOf(projectsPart, projectId)

    suspend fun nextDistributionId(id: String): String {
        return storage.increment(*projectKey(id), distributionCountPart)?.toString() ?: throw IllegalStateException("Increment failed")
    }

    suspend fun create(request: ProjectRequest): Project {
        val id = nextProjectId()

        val project = Project(
            name = id,
            iri = request.manifest.ontola.websiteIRI,
            websiteIRI = request.manifest.ontola.websiteIRI,
            resources = request.resources,
            hextuples = request.hextuples,
            manifest = request.manifest,
        )

        storage.setHashValues(
            *projectKey(id),
            entries = mapOf(
                "name" to id,
                "iri" to project.iri.toString(),
                "websiteIRI" to project.websiteIRI.toString(),
                "resources" to Json.encodeToString(project.resources),
                "hextuples" to Json.encodeToString(project.hextuples),
                "manifest" to Json.encodeToString(project.manifest),
            )
        )
        storage.setAdd(projectSetKey, member = id)

        return project
    }

    suspend fun store(projectId: String, request: ProjectRequest): Project {
        val project = get(projectId) ?: throw NotFoundException()

        val sanitised = project.copy(
            resources = request.resources,
            hextuples = request.hextuples,
            manifest = request.manifest,
        )

        storage.setHashValues(
            *projectKey(projectId),
            entries = mapOf(
                "iri" to sanitised.iri.toString(),
                "websiteIRI" to sanitised.websiteIRI.toString(),
                "resources" to Json.encodeToString(sanitised.resources),
                "hextuples" to Json.encodeToString(sanitised.hextuples),
                "manifest" to Json.encodeToString(sanitised.manifest),
            )
        )
        storage.setAdd(projectSetKey, member = projectId)

        return sanitised
    }

    suspend fun get(projectId: String): Project? {
        val name = storage.getHashValue(*projectKey(projectId), hashKey = "name") ?: return null
        val iri = storage.getHashValue(*projectKey(projectId), hashKey = "iri") ?: return null
        val websiteIRI = storage.getHashValue(*projectKey(projectId), hashKey = "websiteIRI") ?: return null
        val resources = storage.getHashValue(*projectKey(projectId), hashKey = "resources") ?: return null
        val hextuples = storage.getHashValue(*projectKey(projectId), hashKey = "hextuples") ?: return null
        val manifest = storage.getHashValue(*projectKey(projectId), hashKey = "manifest") ?: return null

        return Project(
            name = name,
            iri = Url(iri),
            websiteIRI = Url(websiteIRI),
            resources = Json.decodeFromString(resources),
            hextuples = Json.decodeFromString(hextuples),
            manifest = Json.decodeFromString(manifest),
        )
    }

    @OptIn(FlowPreview::class)
    suspend fun findAll(): Set<String> {
        return storage.getSet(projectsPart) ?: emptySet()
    }

    private suspend fun nextProjectId(): String {
        return storage.increment(projectCountPart)?.toString() ?: throw IllegalStateException("Increment failed")
    }
}
