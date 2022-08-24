package tools.empathy.studio

import io.ktor.http.Url
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.plugins.Storage
import tools.empathy.url.stem
import tools.empathy.url.withTrailingSlash

class PublicationRepo(val storage: Storage) {
    private val publicationsKey = arrayOf(routePart, startsWithPart)

    private suspend fun find(predicate: ((p: Publication) -> Boolean)?): List<Publication> = getAll()
        .filter { predicate?.invoke(it) == true }

    suspend fun getPublicationsOfProject(projectId: String): List<Publication> = find {
        it.projectId == projectId
    }

    /**
     * Searches for a [Publication] which is applicable for the given [route]
     */
    suspend fun match(route: Url): Publication? {
        val stemmed = route.stem()

        return find {
            it.startRoute.toString() == stemmed ||
                stemmed.startsWith(it.startRoute.withTrailingSlash)
        }.firstOrNull()
    }

    suspend fun store(publication: Publication) {
        storage.setHashValues(
            *publicationsKey,
            entries = mapOf(
                publication.startRoute.toString() to Json.encodeToString(
                    Pair(
                        publication.projectId,
                        publication.distributionId,
                    ),
                )
            )
        )
    }

    suspend fun delete(publication: Publication): Boolean {
        val publicationRoute = find { it == publication }
            .firstOrNull()
            ?.startRoute
            ?: return false

        return storage.deleteHashValue(*publicationsKey, hashKey = publicationRoute.toString())
    }

    private suspend fun getAll(): List<Publication> = storage
        .getHash(routePart, startsWithPart)
        .map { (key, value) ->
            val (projectId, distributionId) = Json.decodeFromString<Pair<String, String>>(value)

            Publication(
                startRoute = Url(key),
                projectId = projectId,
                distributionId = distributionId,
            )
        }
}
