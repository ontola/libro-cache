package tools.empathy.libro.server.tenantization

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.Headers
import io.ktor.http.Url
import io.ktor.http.takeFrom
import io.ktor.server.application.ApplicationCall
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.plugins.services
import tools.empathy.libro.server.util.copy

internal fun Headers.proto(): String = get("X-Forwarded-Proto")?.split(',')?.firstOrNull()
    ?: get("scheme")
    ?: get("origin")?.let { Url(it).protocol.name }
    ?: "http"

/**
 * Queries the `find_tenant` SPI endpoint for a given [recordId].
 */
internal suspend fun ApplicationCall.getTenant(recordId: String): TenantFinderResponse {
    return application.libroConfig.client.get {
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
