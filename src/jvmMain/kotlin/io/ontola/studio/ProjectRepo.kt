package io.ontola.studio

import io.ontola.cache.plugins.Storage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val docsPrefix = "docs"
private const val dataPostfix = "data"
private const val manifestKey = "manifest"
private const val sitemapKey = "sitemap"

class ProjectRepo(val storage: Storage) {
    private fun documentKey(projectId: String): Array<String> = arrayOf(docsPrefix, projectId)

    suspend fun nextDistributionId(id: String): String {
        return storage.increment(*documentKey(id))?.toString() ?: throw IllegalStateException("Increment failed")
    }

    suspend fun store(id: String, distribution: Distribution) {
        storage.setAllListValues(docsPrefix, id, dataPostfix, values = distribution.data.map { Json.encodeToString(it) })
        storage.setHashValues(
            docsPrefix,
            id,
            entries = mapOf(
                manifestKey to Json.encodeToString(distribution.manifest),
                sitemapKey to distribution.sitemap,
            ),
        )
    }

    suspend fun get(projectId: String): Project? = storage
        .getString(*documentKey(projectId))
        ?.let { Json.decodeFromString<Project>(it) }
}
