@file:UseSerializers(UrlSerializer::class)

package tools.empathy.libro.server.tenantization

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.plugins.services
import tools.empathy.libro.server.util.UrlSerializer
import tools.empathy.libro.server.util.copy
import tools.empathy.libro.server.util.proxySafeHeaders

@Serializable
data class TenantDescription(
    val name: String,
    val location: Url,
)

@Serializable
data class TenantsResponse(
    val schemas: List<String>,
    val sites: List<TenantDescription>,
)

internal suspend fun ApplicationCall.getTenantsRequest(): HttpResponse = application
    .libroConfig
    .client
    .get(services.route("/_public/spi/tenants")) {
        headers {
            header(HttpHeaders.Authorization, "Bearer ${application.libroConfig.sessions.oAuthToken}")
            proxySafeHeaders(request)
            copy(HttpHeaders.XForwardedFor, request)
            copy("X-Real-Ip", request)
        }
    }

/**
 * Queries the `tenants` SPI endpoint for all the known tenants.
 */
internal suspend fun ApplicationCall.getTenants(): TenantsResponse {
    val response = getTenantsRequest()

    if (response.status != HttpStatusCode.OK) {
        throw ResponseException(response, response.body())
    }

    return response.body()
}
