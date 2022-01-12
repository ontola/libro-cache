package io.ontola.studio

import io.ktor.http.Url
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.plugins.Storage
import io.ontola.rdf.hextuples.Hextuple
import io.ontola.util.stem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val docsPrefix = "docs"
private const val routePrefix = "routes"
private const val startsWith = "start"
private const val data = "data"
private const val wildcard = "*"
private const val manifestKey = "manifest"
private const val sitemapKey = "sitemap"

class DistributionRepo(val storage: Storage) {
    suspend fun store(id: String, distribution: Distribution) {
        storage.setAllListValues(docsPrefix, id, data, values = distribution.data.map { Json.encodeToString(it) })
        storage.setHashValues(
            docsPrefix,
            id,
            entries = mapOf(
                manifestKey to Json.encodeToString(distribution.manifest),
                sitemapKey to distribution.sitemap,
            ),
        )
    }

    suspend fun get(id: String): Distribution? {
        val data = storage.getAllListValues(docsPrefix, id, dataPostfix).map { Json.decodeFromString<Array<String>>(it) }
        val manifest = storage.getHashValue(docsPrefix, id, hashKey = manifestKey) ?: return null
        val sitemap = storage.getHashValue(docsPrefix, id, hashKey = sitemapKey) ?: return null

        return Distribution(
            data = data,
            manifest = Json.decodeFromString(manifest),
            sitemap = sitemap,
        )
    }

    suspend fun getData(id: String): List<Hextuple> {
        return storage.getAllListValues(docsPrefix, id, data)
            .map { Json.decodeFromString(it) }
    }

    suspend fun getManifest(id: String): Manifest? {
        val value = storage.getHashValue(docsPrefix, id, hashKey = "manifestOverride") ?: return null

        return Json.decodeFromString(value)
    }

    suspend fun getSitemap(id: String): String? {
        return storage.getHashValue(docsPrefix, id, hashKey = "sitemap")
    }

    suspend fun documentKeyForRoute(url: Url): String? {
        val allRoutes = storage.keys(routePrefix, startsWith, wildcard)
        val stemmed = url.stem()

        val match = allRoutes
            .firstOrNull { it.last() == stemmed || stemmed.startsWith("${it.last()}/") }
            ?.toTypedArray()
            ?: return null

        return storage.getString(*match)
    }
}
