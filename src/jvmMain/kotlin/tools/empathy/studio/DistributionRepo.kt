package tools.empathy.studio

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.plugins.Storage
import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.sitemap

class DistributionRepo(val storage: Storage) {
    private val lenientJson = Json {
        ignoreUnknownKeys = true
    }

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
                xmlSitemapKey to Json.encodeToString(distribution.xmlSitemap),
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
        val xmlSitemap = storage.getHashValue(*key, hashKey = xmlSitemapKey)
        val version = storage.getHashValue(*key, hashKey = versionKey) ?: return null
        val message = storage.getHashValue(*key, hashKey = messageKey) ?: return null
        val createdAt = storage.getHashValue(*key, hashKey = createdAtKey) ?: return null

        return Distribution(
            data = data,
            manifest = lenientJson.decodeFromString(manifest),
            sitemap = sitemap,
            xmlSitemap = xmlSitemap?.let { lenientJson.decodeFromString(it) } ?: data.sitemap(),
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
    suspend fun find(projectId: String): List<String> {
        val keys = storage.keys(projectsPart, projectId, distributionsPart, wildcard)

        return keys
            .map { it.last() }
            .filter { it != "data" }
            .map { it.toInt() }
            .toList()
            .sortedDescending()
            .map { it.toString() }
    }
}
