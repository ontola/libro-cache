package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ontola.cache.plugins.logger

internal suspend fun ApplicationCall.readAndPartition(
    requested: List<CacheRequest>
): ReadResult {
    val entries = readFromStorage(requested)
    logger.debug { "Fetched ${entries.size} resources from storage" }

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
