package tools.empathy.studio

import io.ktor.http.Url
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.FlowPreview
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.plugins.Storage
import tools.empathy.serialization.normaliseAbsolutePaths

class ProjectRepo(val storage: Storage) {
    private val lenientJson = Json {
        ignoreUnknownKeys = true
    }
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
        val data = storage.getHashValue(*projectKey(projectId), hashKey = "data") ?: return null
        val manifest = storage.getHashValue(*projectKey(projectId), hashKey = "manifest") ?: return null

        return Project(
            name = name,
            iri = Url(iri),
            websiteIRI = Url(websiteIRI),
            data = lenientJson.decodeFromString(data),
            manifest = lenientJson.decodeFromString(manifest),
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
