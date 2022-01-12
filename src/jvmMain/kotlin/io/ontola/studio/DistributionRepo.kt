package io.ontola.studio

import io.ktor.http.Url
import io.ontola.cache.plugins.Storage
import io.ontola.empathy.web.DataSlice
import io.ontola.util.stem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val docsPrefix = "docs"
private const val routePrefix = "routes"
private const val startsWith = "start"
private const val dataPostfix = "data"
private const val wildcard = "*"
private const val manifestKey = "manifest"
private const val sitemapKey = "sitemap"
private const val distributionsKey = "distributions"

class DistributionRepo(val storage: Storage) {
    private fun distributionKey(projectId: String, distributionId: String): Array<String> =
        arrayOf(docsPrefix, projectId, distributionsKey, distributionId)

    suspend fun store(projectId: String, distribution: Distribution) {
        val distId = ProjectRepo(storage).nextDistributionId(projectId)
        storage.setAllListValues(*distributionKey(projectId, distId), dataPostfix, values = distribution.data.map { Json.encodeToString(it) })
        storage.setHashValues(
            *distributionKey(projectId, distId),
            entries = mapOf(
                manifestKey to Json.encodeToString(distribution.manifest),
                sitemapKey to distribution.sitemap,
            ),
        )
    }

    suspend fun get(projectId: String, distId: String): Distribution? {
        val data = getData(projectId, distId)
        val manifest = storage.getHashValue(*distributionKey(projectId, distId), hashKey = manifestKey) ?: return null
        val sitemap = storage.getHashValue(*distributionKey(projectId, distId), hashKey = sitemapKey) ?: return null

        return Distribution(
            data = data,
            manifest = Json.decodeFromString(manifest),
            sitemap = sitemap,
        )
    }

    suspend fun getData(projectId: String, distId: String): DataSlice {
        return storage.getString(*distributionKey(projectId, distId), dataPostfix)
            ?.let { Json.decodeFromString(it) }
            ?: emptyMap()
    }

    /**
     * Find all [Distribution] for the document with [projectId]
     */
    @OptIn(FlowPreview::class)
    suspend fun find(projectId: String): List<String> {
        return storage
            .keys(docsPrefix, projectId, distributionsKey, wildcard)
            .flatMapConcat { it.asFlow() }
            .toList()
    }

    suspend fun distributionPairForRoute(url: Url): Pair<String, String>? {
        val allRoutes = storage.keys(routePrefix, startsWith, wildcard)
        val stemmed = url.stem()

        val match = allRoutes
            .firstOrNull { it.last() == stemmed || stemmed.startsWith("${it.last()}/") }
            ?.toTypedArray()
            ?: return null

        return storage.getString(*match)?.let { Json.decodeFromString(it) }
    }

    suspend fun publishDistributionToRoute(projectId: String, distId: String, startRoute: String) {
        storage.setString(
            routePrefix,
            startsWith,
            startRoute,
            value = Json.encodeToString(Pair(projectId, distId)),
            expiration = null,
        )
    }
}
