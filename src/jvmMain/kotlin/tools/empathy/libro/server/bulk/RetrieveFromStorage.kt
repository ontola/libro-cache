package tools.empathy.libro.server.bulk

import io.ktor.server.application.ApplicationCall
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import tools.empathy.libro.server.plugins.language
import tools.empathy.libro.server.plugins.storage
import tools.empathy.libro.server.util.measured

@OptIn(ExperimentalLettuceCoroutinesApi::class)
internal suspend fun ApplicationCall.readFromStorage(
    requested: List<CacheRequest>,
): Map<String, CacheEntry> = measured("readFromStorage") {
    val lang = language
    val storage = application.storage

    requested
        .map { req -> req.iri to storage.getCacheEntry(req.iri, lang) }
        .mapNotNull { it.second }
        .associateBy { it.iri }
}
