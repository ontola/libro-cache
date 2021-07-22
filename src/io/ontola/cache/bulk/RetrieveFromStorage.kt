package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.ontola.cache.plugins.Storage
import io.ontola.cache.plugins.session
import io.ontola.cache.plugins.storage
import io.ontola.cache.util.measured

@OptIn(ExperimentalLettuceCoroutinesApi::class)
internal suspend fun ApplicationCall.readFromStorage(
    requested: List<CacheRequest>,
): Map<String, CacheEntry> = measured("readFromStorage") {
    val lang = session.language()
    val storage = application.storage

    readFromStorage(requested, storage, lang)
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
internal suspend fun readFromStorage(
    requested: List<CacheRequest>,
    storage: Storage,
    lang: String,
): Map<String, CacheEntry> = requested
    .map { req -> req.iri to storage.getCacheEntry(req.iri, lang) }
    .mapNotNull { it.second }
    .associateBy { it.iri }
