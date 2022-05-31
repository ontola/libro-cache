package io.ontola.studio

import io.ktor.http.Url
import io.ktor.server.plugins.NotFoundException
import io.ontola.cache.plugins.Storage
import io.ontola.empathy.web.normaliseAbsolutePaths
import io.ontola.empathy.web.toSlice
import io.ontola.rdf.hextuples.Hextuple
import kotlinx.coroutines.FlowPreview
import kotlinx.css.data
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
            data = request.data.normaliseAbsolutePaths(),
            manifest = request.manifest,
        )

        storage.setHashValues(
            *projectKey(id),
            entries = mapOf(
                "name" to id,
                "iri" to project.iri.toString(),
                "websiteIRI" to project.websiteIRI.toString(),
                "data" to Json.encodeToString(project.data),
                "manifest" to Json.encodeToString(project.manifest),
            )
        )
        storage.setAdd(projectSetKey, member = id)

        return project
    }

    suspend fun store(projectId: String, request: ProjectRequest): Project {
        val project = get(projectId) ?: throw NotFoundException()

        val sanitised = project.copy(
            data = request.data.normaliseAbsolutePaths(),
            manifest = request.manifest,
        )

        storage.setHashValues(
            *projectKey(projectId),
            entries = mapOf(
                "iri" to sanitised.iri.toString(),
                "websiteIRI" to sanitised.websiteIRI.toString(),
                "data" to Json.encodeToString(sanitised.data),
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
        val hextuples = storage.getHashValue(*projectKey(projectId), hashKey = "hextuples")
        val data = storage.getHashValue(*projectKey(projectId), hashKey = "data")
        val manifest = storage.getHashValue(*projectKey(projectId), hashKey = "manifest") ?: return null

        if (hextuples == null && data == null) {
            return null
        }

        val dataWithFallback = data?.let { Json.decodeFromString(it) }
            ?: Json.decodeFromString<List<Hextuple>>(hextuples!!).toSlice(Url(websiteIRI))

        return Project(
            name = name,
            iri = Url(iri),
            websiteIRI = Url(websiteIRI),
            data = dataWithFallback,
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
