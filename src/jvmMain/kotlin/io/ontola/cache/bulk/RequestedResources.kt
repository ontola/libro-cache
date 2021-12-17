package io.ontola.cache.bulk

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.util.pipeline.PipelineContext
import java.net.URLDecoder
import java.nio.charset.Charset

internal suspend fun PipelineContext<Unit, ApplicationCall>.requestedResources(): List<CacheRequest> {
    val params = call.receiveParameters()
    val resources = params.getAll("resource[]")

    return resources
        ?.map { r -> CacheRequest(URLDecoder.decode(r, Charset.defaultCharset().name())) }
        ?: emptyList()
}
