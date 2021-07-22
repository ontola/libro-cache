package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.response.header
import io.ktor.response.respondBytes
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.session
import io.ontola.cache.plugins.storage
import io.ontola.cache.util.measured
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import kotlin.time.ExperimentalTime

@OptIn(KtorExperimentalLocationsAPI::class)
@Location("/link-lib/bulk")
class Bulk

val hexJson = ContentType.parse("application/hex+x-ndjson")

@OptIn(ExperimentalTime::class)
fun bulkHandler(): suspend PipelineContext<Unit, ApplicationCall>.(Bulk) -> Unit = {
    var updatedEntries: List<CacheEntry>? = null

    call.measured("handler hot") {
        val requested = call.requestedResources()
        call.logger.debug { "Fetching ${requested.size} resources from cache" }

        val result = call.readAndPartition(requested)
        call.response.header("Link-Cache", result.stats.toString())

        if (result.entirelyPublic) {
            call.logger.debug { "All ${requested.size} resources in cache" }
        }

        val output = ByteArrayOutputStream()
        output.writer(Charset.defaultCharset()).use {
            updatedEntries = call.readOrFetch(result, it)
        }
        call.respondBytes(output.toByteArray(), hexJson, HttpStatusCode.OK)
    }

    call.measured("handler cold") {
        updatedEntries?.let {
            if (it.isNotEmpty()) {
                call.logger.debug { "Updating redis after responding (${it.size} entries)" }
                call.application.storage.setCacheEntries(it, call.session.language())
            }
        }
    }
}
