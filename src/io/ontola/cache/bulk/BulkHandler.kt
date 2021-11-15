package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.response.header
import io.ktor.response.respondOutputStream
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.plugins.storage
import io.ontola.cache.util.measured
import java.io.OutputStream
import java.nio.charset.Charset
import kotlin.time.ExperimentalTime

@OptIn(KtorExperimentalLocationsAPI::class)
@Location("/link-lib/bulk")
class Bulk

val hexJson = ContentType.parse("application/hex+x-ndjson")

suspend fun PipelineContext<Unit, ApplicationCall>.resourcesToOutputStream(resources: List<CacheRequest>, outStream: OutputStream): List<CacheEntry>? {
    val result = readAndPartition(resources)
    call.response.header("Link-Cache", result.stats.toString())

    if (result.entirelyPublic) {
        call.logger.debug { "All ${resources.size} resources in cache" }
    }

    val updatedEntries = outStream.writer(Charset.defaultCharset()).use {
        readOrFetch(result, it)
    }

    return updatedEntries
}

suspend fun PipelineContext<Unit, ApplicationCall>.coldHandler(updatedEntries: List<CacheEntry>?) = measured("handler cold") {
    updatedEntries?.let {
        if (it.isNotEmpty()) {
            call.logger.debug { "Updating redis after responding (${it.size} entries)" }
            call.application.storage.setCacheEntries(it, call.sessionManager.language)
        }
    }
}

@OptIn(ExperimentalTime::class)
fun bulkHandler(): suspend PipelineContext<Unit, ApplicationCall>.(Bulk) -> Unit = {
    var updatedEntries: List<CacheEntry>? = null

    measured("handler hot") {
        val requested = requestedResources()
        call.logger.debug { "Fetching ${requested.size} resources from cache" }

        call.respondOutputStream(hexJson, HttpStatusCode.OK) {
            updatedEntries = resourcesToOutputStream(requested, this)
        }
    }

    coldHandler(updatedEntries)
}
