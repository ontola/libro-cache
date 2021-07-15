package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.response.respondText
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.features.logger
import io.ontola.cache.features.session
import io.ontola.cache.features.storage
import io.ontola.cache.util.measured

@OptIn(KtorExperimentalLocationsAPI::class)
@Location("/link-lib/bulk")
class Bulk

fun bulkHandler(): suspend PipelineContext<Unit, ApplicationCall>.(Bulk) -> Unit = {
    var updatedEntries: List<CacheEntry>? = null

    measured("handler hot") {
        // TODO: handle empty request
        val requested = requestedResources()
        call.logger.debug { "Fetching ${requested.size} resources from cache" }

        val (entries, updates) = readOrFetch(requested)
        updatedEntries = updates
        if (entries.size != requested.size) {
            call.logger.warn("Requested ${requested.size}, serving ${entries.size}")
        } else {
            val diffPos = requested.map { it.iri } - entries.map { it.key }
            val diffNeg = entries.map { it.key } - requested.map { it.iri }
            call.logger.warn("Request / serve diff ${diffPos + diffNeg}")
        }

        call.respondText(
            entries.toList().joinToString("\n") { (_, r) -> "${statusCode(r.iri, r.status)}\n${r.contents ?: ""}" },
            ContentType.parse("application/hex+x-ndjson"),
        )
    }

    measured("handler cold") {
        updatedEntries?.let {
            if (it.isNotEmpty()) {
                call.logger.debug { "Updating redis after responding (${it.size} entries)" }
                call.application.storage.setCacheEntries(it, call.session.language())
            }
        }
    }
}
