package io.ontola.cache.tenantization

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.Headers
import io.ktor.http.Url
import io.ktor.http.takeFrom
import io.ktor.server.application.ApplicationCall
import io.ktor.util.InternalAPI
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.services
import io.ontola.cache.util.copy

internal fun Headers.proto(): String = get("X-Forwarded-Proto")?.split(',')?.firstOrNull()
    ?: get("scheme")
    ?: get("origin")?.let { Url(it).protocol.name }
    ?: "http"

/**
 * Queries the `find_tenant` SPI endpoint for a given [recordId].
 */
internal suspend fun ApplicationCall.getTenant(recordId: String): TenantFinderResponse {
    return application.cacheConfig.client.get {
        url.apply {
            takeFrom(services.route("/_public/spi/find_tenant"))
            parameters["iri"] = recordId
        }
        headers {
            copy("X-Request-Id", request)
        }
    }.body<TenantFinderResponse>().apply {
        val proto = request.headers.proto()
        websiteBase = "$proto://$iriPrefix"
    }
}
