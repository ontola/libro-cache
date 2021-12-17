package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.plugins.storage
import io.ontola.cache.util.measured

@OptIn(ExperimentalLettuceCoroutinesApi::class)
internal suspend fun PipelineContext<Unit, ApplicationCall>.readFromStorage(
    requested: List<CacheRequest>,
): Map<String, CacheEntry> = measured("readFromStorage") {
    val lang = call.sessionManager.language
    val storage = call.application.storage

    requested
        .map { req -> req.iri to storage.getCacheEntry(req.iri, lang) }
        .mapNotNull { it.second }
        .associateBy { it.iri }
}
