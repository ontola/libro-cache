package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.ontola.cache.features.session
import io.ontola.cache.features.storage

@OptIn(ExperimentalLettuceCoroutinesApi::class)
internal suspend fun PipelineContext<Unit, ApplicationCall>.readFromStorage(
    requested: List<CacheRequest>,
): MutableMap<String, CacheEntry> {
    val lang = call.session.language()
    val storage = call.application.storage

    return requested
        .map { req -> req.iri to storage.getCacheEntry(req.iri, lang) }
        .filter { it.second != null }
        .associateBy({ it.first }, { it.second!! })
        .toMutableMap()
}
