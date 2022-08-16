package tools.empathy.libro.server.bulk

import io.ktor.http.URLBuilder
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.util.pipeline.PipelineContext
import tools.empathy.url.asHrefString
import java.net.URLDecoder
import java.nio.charset.Charset

internal suspend fun PipelineContext<Unit, ApplicationCall>.requestedResources(): List<CacheRequest> {
    val params = call.receiveParameters()
    val resources = params.getAll("resource[]")

    return resources
        ?.map { r ->
            val docIRI = URLBuilder(URLDecoder.decode(r, Charset.defaultCharset().name())).apply {
                fragment = ""
            }.build().asHrefString
            CacheRequest(docIRI)
        }
        ?: emptyList()
}
