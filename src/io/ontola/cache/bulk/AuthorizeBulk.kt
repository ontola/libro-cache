package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.services
import io.ontola.cache.plugins.session
import io.ontola.cache.plugins.tenant
import io.ontola.cache.util.measured
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal suspend fun ApplicationCall.authorizeBulk(
    resources: List<String>,
): List<SPIResourceResponseItem> = measured("authorizeBulk;i=${resources.size}") {
    val lang = session.language()
    val prefix = tenant.websiteIRI.encodedPath.split("/").getOrNull(1)?.let { "/$it" } ?: ""

    val res: String = application.cacheConfig.client.post {
        url(services.route("$prefix/spi/bulk"))
        contentType(ContentType.Application.Json)
        initHeaders(this@authorizeBulk, lang)
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

    Json.decodeFromString(res)
}
