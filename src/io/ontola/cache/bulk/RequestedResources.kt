package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.request.receiveParameters
import java.net.URLDecoder
import java.nio.charset.Charset

internal suspend fun ApplicationCall.requestedResources(): List<CacheRequest> {
    val params = receiveParameters()
    val resources = params.getAll("resource[]")

    return resources
        ?.map { r -> CacheRequest(URLDecoder.decode(r, Charset.defaultCharset().name())) }
        ?: emptyList()
}
