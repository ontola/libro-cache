package io.ontola.cache.bulk

import io.ktor.server.application.ApplicationCall
import io.ontola.cache.plugins.language
import io.ontola.cache.plugins.storage
import io.ontola.cache.util.measured
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

internal suspend fun ApplicationCall.readFromStorage(
    requested: List<CacheRequest>,
): Map<String, CacheEntry> = measured("readFromStorage") {
    val lang = language
    val storage = application.storage

    requested
        .parallelStream()
        .map { req ->
            runBlocking(Dispatchers.IO) {
                req.iri to storage.getCacheEntry(req.iri, lang)
            }
        }
        .toList()
        .mapNotNull { it.second }
        .associateBy { it.iri }
}
