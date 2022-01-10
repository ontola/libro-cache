package io.ontola.cache.studio

import io.ktor.http.Url
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.plugins.Storage
import io.ontola.rdf.hextuples.Hextuple
import io.ontola.util.stem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val docsPrefix = "docs"
private const val routePrefix = "routes"
private const val startsWith = "start"
private const val wildcard = "*"

class DocumentRepo(val storage: Storage) {
    suspend fun getData(id: String): List<Hextuple> {
        return storage.getAllListValues(docsPrefix, id)
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
