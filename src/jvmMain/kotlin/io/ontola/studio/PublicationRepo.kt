package io.ontola.studio

import io.ktor.http.Url
import io.ontola.cache.plugins.Storage
import io.ontola.util.stem
import io.ontola.util.withTrailingSlash
import kotlinx.coroutines.FlowPreview
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PublicationRepo(val storage: Storage) {
    private val publicationsKey = arrayOf(routePart, startsWithPart)

    @OptIn(FlowPreview::class)
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
