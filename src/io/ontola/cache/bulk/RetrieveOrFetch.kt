package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.services
import io.ontola.cache.util.scopeBlankNodes

private fun CacheEntry?.isEmptyOrNotPublic() =
    this == null || cacheControl != CacheControl.Public || contents.isNullOrEmpty()

data class Stats(val total: Int, val cached: Int, val public: Int, val authorized: Int)

internal suspend fun PipelineContext<Unit, ApplicationCall>.readOrFetch(
    requested: List<CacheRequest>
): Triple<MutableMap<String, CacheEntry>, List<CacheEntry>?, Stats> {
    val entries = readFromStorage(requested)
    call.logger.debug { "Fetched ${entries.size} resources from storage" }

    val (toRequest, public) = requested.partition { entries[it.iri].isEmptyOrNotPublic() }
    val stats = Stats(requested.size, entries.size, public.size, toRequest.size)

    if (toRequest.isEmpty()) {
        call.logger.debug { "All ${requested.size} resources in cache" }

        return Triple(entries, null, stats)
    }

    call.logger.debug { "Requesting ${toRequest.size} resources" }
    call.logger.trace { "Requesting ${toRequest.joinToString(", ") { it.iri }}" }

    val redisUpdate = toRequest
        .groupBy { call.services.resolve(Url(it.iri).fullPath) }
        .flatMap { (service, resources) ->
            if (service.bulk) {
                authorizeBulk(resources.map { e -> e.iri })
            } else {
                authorizePlain(resources.map { e -> e.iri })
            }
        }
        .map {
            val entry = CacheEntry(
                iri = it.iri,
                status = HttpStatusCode.fromValue(it.status),
                cacheControl = it.cache,
                contents = scopeBlankNodes(it.body),
            )
            entries[it.iri] = entry

            entry
        }
        .filter { it.cacheControl != CacheControl.Private }

    return Triple(entries, redisUpdate, stats)
}
