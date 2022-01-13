package io.ontola.studio

import io.ktor.http.Url
import io.ktor.server.plugins.NotFoundException
import io.ontola.cache.plugins.Storage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val projectPrefix = "projects"
private const val projectCount = "projectCount"
private const val distributionCount = "distributionCount"
private const val dataPostfix = "data"
private const val manifestKey = "manifest"
private const val sitemapKey = "sitemap"

class ProjectRepo(val storage: Storage) {
    private fun projectKey(projectId: String): Array<String> = arrayOf(projectPrefix, projectId)

    suspend fun nextDistributionId(id: String): String {
        return storage.increment(*projectKey(id), distributionCount)?.toString() ?: throw IllegalStateException("Increment failed")
    }

    suspend fun create(request: ProjectRequest): Project {
        val id = nextProjectId()

        val project = Project(
            name = id,
            iri = Url("localhost"),
            websiteIRI = Url("localhost"),
            resources = request.resources,
            hextuples = request.hextuples,
        )

        storage.setString(*projectKey(id), value = Json.encodeToString(project), expiration = null)

        return project
    }

    suspend fun store(projectId: String, request: ProjectRequest): Project {
        val project = get(projectId) ?: throw NotFoundException()

        val sanitised = project.copy(
            iri = Url("localhost"),
            websiteIRI = Url("localhost"),
            resources = request.resources,
            hextuples = request.hextuples,
        )
        storage.setString(*projectKey(projectId), value = Json.encodeToString(sanitised), expiration = null)

        return sanitised
    }

    suspend fun get(projectId: String): Project? = storage
        .getString(*projectKey(projectId))
        ?.let { Json.decodeFromString<Project>(it) }

    @OptIn(FlowPreview::class)
    suspend fun findAll(): List<List<String>> {
        return storage.keys(projectPrefix, "[^:]")
            .toList()
    }

    private suspend fun nextProjectId(): String {
        return storage.increment(projectCount)?.toString() ?: throw IllegalStateException("Increment failed")
    }
}
