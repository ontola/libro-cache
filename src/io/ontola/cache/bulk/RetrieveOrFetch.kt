package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.services
import io.ontola.cache.util.scopeBlankNodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.Writer

internal suspend fun ApplicationCall.readOrFetch(
    result: ReadResult,
    writer: Writer,
): List<CacheEntry>? = coroutineScope {
    val writePublic = async(Dispatchers.IO) {
        result.cachedPublic.forEach {
            writer.write(it)
        }
    }
    val writeAuthorized = async(Dispatchers.IO) {
        if (result.entirelyPublic) {
            return@async null
        }

        val toAuthorize = result.notCached + result.cachedNotPublic

        logger.debug { "Requesting ${toAuthorize.size} resources" }
        logger.trace { "Requesting ${toAuthorize.joinToString(", ") { it.iri }}" }

        val entries = authorize(toAuthorize)

        entries.forEach { writer.write(it) }

        return@async entries.filter { it.cacheControl != CacheControl.Private }
    }
    val (_, redisUpdate) = awaitAll(writePublic, writeAuthorized)

    if (result.entirelyPublic) {
        null
    } else {
        redisUpdate as List<CacheEntry>
    }
}

private suspend fun ApplicationCall.authorize(toAuthorize: List<CacheRequest>): List<CacheEntry> {
    return toAuthorize
        .groupBy { services.resolve(Url(it.iri).fullPath) }
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

private fun Writer.write(entry: CacheEntry) {
    write("${statusCode(entry.iri, entry.status)}\n")
    entry.contents?.let { contents ->
        write("$contents\n")
    }
}
