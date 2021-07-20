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
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
private fun CacheEntry?.isEmptyOrNotPublic(): Boolean {
    contract { returns(false) implies (this@isEmptyOrNotPublic != null) }

    return this == null || isNotPublic() || contents.isNullOrEmpty()
}

private fun CacheEntry.isNotPublic(): Boolean {
    return cacheControl != CacheControl.Public
}

data class Stats(val cached: Int, val public: Int, val authorized: Int) {
    val total: Int = cached + public + authorized
}

internal class ReadResult {
    private val _notCached = mutableListOf<CacheRequest>()
    var notCached: List<CacheRequest> = _notCached
        get() = field.toList()

    private val _cachedNotPublic = mutableListOf<CacheEntry>()
    var cachedNotPublic: List<CacheEntry> = _cachedNotPublic
        get() = field.toList()

    private val _cachedPublic = mutableListOf<CacheEntry>()
    var cachedPublic: List<CacheEntry> = _cachedPublic
        get() = field.toList()

    val entirelyPublic: Boolean
        get() = _notCached.isEmpty() && _cachedNotPublic.isEmpty()

    val stats: Stats
        get() = Stats(
            cached = cachedNotPublic.size + cachedPublic.size,
            authorized = notCached.size,
            public = cachedPublic.size,
        )

    internal fun add(entry: CacheEntry) {
        if (entry.isNotPublic()) {
            _cachedNotPublic.add(entry)
        } else {
            _cachedPublic.add(entry)
        }
    }

    internal fun add(entry: CacheRequest) {
        _notCached.add(entry)
    }
}

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

suspend fun PipelineContext<Unit, ApplicationCall>.authorize(toAuthorize: List<CacheRequest>): List<CacheEntry> {
    return toAuthorize
        .groupBy { call.services.resolve(Url(it.iri).fullPath) }
        .flatMap { (service, resources) ->
            if (service.bulk) {
                authorizeBulk(resources.map { e -> e.iri })
            } else {
                authorizePlain(resources.map { e -> e.iri })
            }
        }
        .map {
            CacheEntry(
                iri = it.iri,
                status = HttpStatusCode.fromValue(it.status),
                cacheControl = it.cache,
                contents = scopeBlankNodes(it.body),
            )
        }
}

internal suspend fun PipelineContext<Unit, ApplicationCall>.readOrFetch(
    requested: List<CacheRequest>
): Triple<List<CacheEntry>, List<CacheEntry>?, Stats> {
    val result = readAndPartition(requested)

    if (result.entirelyPublic) {
        call.logger.debug { "All ${requested.size} resources in cache" }

        return Triple(result.cachedPublic, null, result.stats)
    }

    val toAuthorize = result.notCached + result.cachedNotPublic

    call.logger.debug { "Requesting ${toAuthorize.size} resources" }
    call.logger.trace { "Requesting ${toAuthorize.joinToString(", ") { it.iri }}" }

    val entries = authorize(toAuthorize)
    val redisUpdate = entries.filter { it.cacheControl != CacheControl.Private }

    return Triple(result.cachedPublic + entries, redisUpdate, result.stats)
}
