package io.ontola.cache.bulk

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.logger

internal suspend fun PipelineContext<Unit, ApplicationCall>.readAndPartition(
    requested: List<CacheRequest>
): ReadResult {
    val entries = readFromStorage(requested)
    call.logger.debug { "Fetched ${entries.size} resources from storage" }

    return ReadResult().apply {
        requested.forEach { requested ->
            entries[requested.iri].let {
                if (it.isEmptyOrNotPublic()) {
                    add(requested)
                } else {
                    add(it)
                }
            }
        }
    }
}
