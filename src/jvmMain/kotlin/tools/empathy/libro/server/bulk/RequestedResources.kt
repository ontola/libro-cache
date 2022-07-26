package tools.empathy.libro.server.bulk

import io.ktor.http.URLBuilder
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveParameters
import io.ktor.util.AttributeKey
import kotlinx.css.map
import tools.empathy.url.withoutTrailingSlash
import java.net.URLDecoder
import java.nio.charset.Charset

private val BulkResourcesKey = AttributeKey<List<CacheRequest>>("BulkResourcesKey")

internal suspend fun ApplicationCall.requestedResources(): List<CacheRequest> {
    if (attributes.contains(BulkResourcesKey))
        return attributes[BulkResourcesKey]

    val params = receiveParameters()
    val resources = params.getAll("resource[]")

    val cacheRequests = resources
        ?.map { r ->
            val docIRI = URLBuilder(URLDecoder.decode(r, Charset.defaultCharset().name())).apply {
                fragment = ""
            }.build().withoutTrailingSlash
            CacheRequest(docIRI)
        }
        ?: emptyList()

    return cacheRequests.also { attributes.put(BulkResourcesKey, it) }
}
