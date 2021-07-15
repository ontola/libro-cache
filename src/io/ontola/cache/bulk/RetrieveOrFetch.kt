package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.features.cacheConfig
import io.ontola.cache.features.logger
import io.ontola.cache.features.services
import io.ontola.cache.features.session
import io.ontola.cache.util.KeyManager
import io.ontola.cache.util.scopeBlankNodes

private fun CacheEntry?.isEmptyOrNotPublic() =
    this == null || cacheControl != CacheControl.Public || contents.isNullOrEmpty()

internal suspend fun PipelineContext<Unit, ApplicationCall>.retrieveOrFetch(
    requested: List<CacheRequest>
): Pair<MutableMap<String, CacheEntry>, Map<String, Map<String, String>>?> {
    val config = call.application.cacheConfig
    val lang = call.session.language()
    val keyManager = KeyManager(config)

    val entries = retrieveFromStorage(requested, keyManager)
    call.logger.debug { "Fetched ${entries.size} resources from storage" }

    val toRequest = requested.filter { entries[it.iri].isEmptyOrNotPublic() }

    if (toRequest.isEmpty()) {
        call.logger.debug("All ${requested.size} resources in cache")

        return Pair(entries, null)
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
        .associateBy(
            { keyManager.toKey(it.iri, lang) },
            {
                mapOf(
                    "iri" to it.iri,
                    "status" to it.status.value.toString(10),
                    "cacheControl" to it.cacheControl.toString(),
                    "contents" to it.contents.orEmpty(),
                )
            }
        )

    return Pair(entries, redisUpdate)
}
