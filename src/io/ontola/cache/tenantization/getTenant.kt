package io.ontola.cache.tenantization

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.Headers
import io.ktor.http.Url
import io.ktor.http.takeFrom
import io.ktor.util.InternalAPI
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.services
import io.ontola.cache.util.copy

fun Headers.proto(): String = get("X-Forwarded-Proto")?.split(',')?.firstOrNull()
    ?: get("scheme")
    ?: get("origin")?.let { Url(it).protocol.name }
    ?: "http"

@OptIn(InternalAPI::class)
internal suspend fun PipelineContext<*, ApplicationCall>.getTenant(resourceIri: String): TenantFinderResponse {
    return application.cacheConfig.client.get<TenantFinderResponse> {
        url.apply {
            takeFrom(call.services.route("/_public/spi/find_tenant"))
            parameters["iri"] = resourceIri
        }
        headers {
            copy("X-Request-Id", context.request)
        }
    }.apply {
        val proto = context.request.headers.proto()
        websiteBase = "$proto://$iriPrefix"
    }
}
