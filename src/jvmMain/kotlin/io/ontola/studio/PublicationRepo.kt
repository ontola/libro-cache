package io.ontola.studio

import io.ktor.http.Url
import io.ontola.cache.plugins.Storage
import io.ontola.cache.util.KeyMap
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val url = "url"

class PublicationRepo(val storage: Storage) {
    private val publicationKeyMap = KeyMap(routePart, startsWithPart, url)

    private fun publicationKey(route: Url) = arrayOf(routePart, startsWithPart, route.toString())

    @OptIn(FlowPreview::class)
    suspend fun find(predicate: ((p: Publication) -> Boolean)?): List<Publication> {
        val keysFlow = storage.keys(routePart, startsWithPart, wildcard)
        val publications = keysFlow.mapNotNull {
            val value = storage.getString(*it.toTypedArray()) ?: return@mapNotNull null
            val distributionPair = Json.decodeFromString<Pair<String, String>>(value)

            val pub = Publication(
                startRoute = Url(publicationKeyMap.getPart(it, url)),
                projectId = distributionPair.first,
                distributionId = distributionPair.second,
            )

            return@mapNotNull if (predicate?.invoke(pub) == true) pub else null
        }.toList()

        return publications
    }

    suspend fun getPublicationsOfProject(projectId: String): List<Publication> = find() {
        it.projectId == projectId
    }

    suspend fun store(publication: Publication) {
        storage.setString(
            routePart,
            startsWithPart,
            publication.startRoute.toString(),
            value = Json.encodeToString(
                Pair(
                    publication.projectId,
                    publication.distributionId,
                ),
            ),
            expiration = null,
        )
    }

    suspend fun delete(publication: Publication): Boolean {
        val foundPublications = find() {
            it == publication
        }

        if (foundPublications.isEmpty()) return false

        val deleted = storage.deleteKey(*publicationKey(foundPublications[0].startRoute)) ?: 0

        return deleted > 0
    }
}
