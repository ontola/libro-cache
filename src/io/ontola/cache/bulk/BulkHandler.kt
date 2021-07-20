package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.response.header
import io.ktor.response.respondText
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.session
import io.ontola.cache.plugins.storage
import io.ontola.cache.util.measured
import java.util.Locale
import kotlin.time.ExperimentalTime

@OptIn(KtorExperimentalLocationsAPI::class)
@Location("/link-lib/bulk")
class Bulk

fun ratio(tot: Int, part: Int): String {
    if (tot == 0) {
        return "1.00"
    } else if (part == 0) {
        return "0.00"
    }

    return "%.2f".format(Locale.ENGLISH, part.toDouble() / tot)
}

fun PipelineContext<Unit, ApplicationCall>.checkInvariant(requested: List<CacheRequest>, entries: List<CacheEntry>) {
    if (entries.size != requested.size) {
        call.logger.warn("Requested ${requested.size}, serving ${entries.size}")
    } else {
        val diffPos = requested.map { it.iri } - entries.map { it.iri }
        val diffNeg = entries.map { it.iri } - requested.map { it.iri }
        call.logger.warn("Request / serve diff ${diffPos + diffNeg}")
    }
}

@OptIn(ExperimentalTime::class)
fun bulkHandler(): suspend PipelineContext<Unit, ApplicationCall>.(Bulk) -> Unit = {
    val updatedEntries = measured("handler hot") {
        val requested = requestedResources()
        call.logger.debug { "Fetching ${requested.size} resources from cache" }

        val (entries, updates, stats) = readOrFetch(requested)
        call.response.header(
            "Link-Cache",
            "items=${entries.size}; cached=${ratio(stats.total, stats.cached)}; public=${ratio(stats.total, stats.public)}; authorized=${ratio(stats.total, stats.authorized)}; ",
        )
        checkInvariant(requested, entries)

        call.respondText(
            entries.joinToString("\n") { r -> "${statusCode(r.iri, r.status)}\n${r.contents ?: ""}" },
            ContentType.parse("application/hex+x-ndjson"),
        )

        updates
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
