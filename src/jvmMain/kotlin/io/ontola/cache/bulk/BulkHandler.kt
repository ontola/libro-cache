package io.ontola.cache.bulk

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.language
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.storage
import io.ontola.cache.util.measured
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList

val hexJson = ContentType.parse("application/hex+x-ndjson")
val ndEmpJson = ContentType.parse("application/empathy+x-ndjson")

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun PipelineContext<Unit, ApplicationCall>.collectResources(resources: List<CacheRequest>): Flow<CacheEntry> {
    val result = readAndPartition(resources)
    call.response.header("Link-Cache", result.stats.toString())

    if (result.entirelyPublic) {
        call.logger.debug { "All ${resources.size} resources in cache" }

        return result.cachedPublic.asFlow()
    }

    val toAuthorize = result.notCached + result.cachedNotPublic

    call.logger.debug { "Requesting ${toAuthorize.size} resources" }
    call.logger.trace { "Requesting ${toAuthorize.joinToString(", ") { it.iri }}" }

    val entries = authorize(toAuthorize.asFlow())

    return merge(result.cachedPublic.asFlow(), entries)
}

suspend fun PipelineContext<Unit, ApplicationCall>.coldHandler(updatedEntries: List<CacheEntry>?) = measured("handler cold") {
    updatedEntries?.let {
        if (it.isNotEmpty()) {
            call.logger.debug { "Updating redis after responding (${it.size} entries)" }
            call.application.storage.setCacheEntries(it, call.language)
        }
    }
}

fun Routing.mountBulkHandler() {
    post("/link-lib/bulk") {
        if (call.attributes.contains(AttributeKey<Unit>("StatusPagesTriggered"))) {
            return@post
        }

        val updatedEntries: List<CacheEntry>? = measured("handler hot") {
            val requested = requestedResources()
            call.logger.debug { "Fetching ${requested.size} resources from cache" }

            collectResources(requested)
                .also {
                    call.respondOutputStream(ndEmpJson, HttpStatusCode.OK) {
                        entriesToOutputStream(it, this)
                    }
                }
                .filter { it.cacheControl != CacheControl.Private && it.status == HttpStatusCode.OK }
                .toList()
        }

        coldHandler(updatedEntries)
    }
}
