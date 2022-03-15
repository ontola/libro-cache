package io.ontola.studio

import io.ontola.cache.plugins.Storage
import io.ontola.empathy.web.DataSlice
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DistributionRepo(val storage: Storage) {
    private fun distributionKey(projectId: String, distributionId: String): Array<String> =
        arrayOf(projectsPart, projectId, distributionsPart, distributionId)

    suspend fun store(projectId: String, distribution: Distribution) {
        val distId = ProjectRepo(storage).nextDistributionId(projectId)
        val key = distributionKey(projectId, distId)

        storage.setHashValues(
            *key,
            entries = mapOf(
                manifestKey to Json.encodeToString(distribution.manifest),
                sitemapKey to distribution.sitemap,
                versionKey to distribution.meta.version,
                messageKey to distribution.meta.message,
                createdAtKey to distribution.meta.createdAt.toString(),
                dataKey to Json.encodeToString(distribution.data),
            ),
        )
    }

    suspend fun get(projectId: String, distId: String): Distribution? {
        val key = distributionKey(projectId, distId)
        val data = getData(projectId, distId)
        val manifest = storage.getHashValue(*key, hashKey = manifestKey) ?: return null
        val sitemap = storage.getHashValue(*key, hashKey = sitemapKey) ?: return null
        val version = storage.getHashValue(*key, hashKey = versionKey) ?: return null
        val message = storage.getHashValue(*key, hashKey = messageKey) ?: return null
        val createdAt = storage.getHashValue(*key, hashKey = createdAtKey) ?: return null

        return Distribution(
            data = data,
            manifest = Json.decodeFromString(manifest),
            sitemap = sitemap,
            meta = DistributionMeta(
                version = version,
                message = message,
                createdAt = createdAt.toLong(),
                live = false,
            ),
        )
    }

    private suspend fun getData(projectId: String, distId: String): DataSlice {
        return storage.getHashValue(*distributionKey(projectId, distId), hashKey = dataKey)
            ?.let { Json.decodeFromString(it) }
            ?: emptyMap()
    }

    /**
     * Find all [Distribution] for the document with [projectId]
     */
    @OptIn(FlowPreview::class)
    suspend fun find(projectId: String): List<String> {
        val keys = storage.keys(projectsPart, projectId, distributionsPart, wildcard)

        return keys.map { it.last() }
            .filter { it != "data" }
            .toList()
    }
}
