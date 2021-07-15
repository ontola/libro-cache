package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.features.cacheConfig
import io.ontola.cache.features.services
import io.ontola.cache.features.session
import io.ontola.cache.features.tenant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal suspend fun PipelineContext<Unit, ApplicationCall>.authorizeBulk(
    resources: List<String>,
): List<SPIResourceResponseItem> {
    val lang = call.session.language()
    val prefix = call.tenant.websiteIRI.encodedPath.split("/").getOrNull(1)?.let { "/$it" } ?: ""

    val res: String = call.application.cacheConfig.client.post {
        url(call.services.route("$prefix/spi/bulk"))
        contentType(ContentType.Application.Json)
        initHeaders(call, lang)
        headers {
            header("Accept", ContentType.Application.Json)
            header("Content-Type", ContentType.Application.Json)
        }
        body = SPIAuthorizeRequest(
            resources = resources.map { r ->
                SPIResourceRequestItem(
                    iri = r,
                    include = true,
                )
            }
        )
    }

    return Json.decodeFromString(res)
}
