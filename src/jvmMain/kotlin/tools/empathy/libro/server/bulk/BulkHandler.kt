package tools.empathy.libro.server.bulk

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList
import tools.empathy.libro.server.plugins.language
import tools.empathy.libro.server.plugins.logger
import tools.empathy.libro.server.plugins.storage
import tools.empathy.libro.server.util.measured

val ndEmpJson = ContentType.parse("application/empathy+x-ndjson")

suspend fun ApplicationCall.collectResources(resources: List<CacheRequest>): Flow<CacheEntry> {
    val result = readAndPartition(resources)
    response.header("Link-Cache", result.stats.toString())

    if (result.entirelyPublic) {
        logger.debug { "All ${resources.size} resources in cache" }

        return result.cachedPublic.asFlow()
    }

    val toAuthorize = result.notCached + result.cachedNotPublic

    logger.debug { "Requesting ${toAuthorize.size} resources" }
    logger.trace { "Requesting ${toAuthorize.joinToString(", ") { it.iri }}" }

    val entries = authorize(toAuthorize.asFlow())

    return merge(result.cachedPublic.asFlow(), entries)
}

suspend fun ApplicationCall.coldHandler(updatedEntries: List<CacheEntry>?) = measured("handler cold") {
    updatedEntries?.let {
        if (it.isNotEmpty()) {
            logger.debug { "Updating redis after responding (${it.size} entries)" }
            application.storage.setCacheEntries(it, language)
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.handleBulk() {
    if (call.attributes.contains(AttributeKey<Unit>("StatusPagesTriggered"))) {
        return
    }

    val updatedEntries: List<CacheEntry> = call.measured("handler hot") {
        val requested = requestedResources()
        call.logger.debug { "Fetching ${requested.size} resources from cache" }

        call.collectResources(requested)
            .also {
                call.respondOutputStream(ndEmpJson, HttpStatusCode.OK) {
                    entriesToOutputStream(it, this)
                }
            }
            .filter { it.cacheControl != CacheControl.Private && it.status == HttpStatusCode.OK }
            .toList()
    }

    call.coldHandler(updatedEntries)
}

fun Routing.mountBulkHandler() {
    post("/link-lib/bulk") {
        handleBulk()
    }

    post("/{site}/link-lib/bulk") {
        handleBulk()
    }
}
