@file:UseSerializers(UrlSerializer::class)
package io.ontola.cache.tenantization

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.services
import io.ontola.cache.util.UrlSerializer
import io.ontola.cache.util.copy
import io.ontola.cache.util.proxySafeHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

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

internal suspend fun ApplicationCall.getTenants(): TenantsResponse {
    val request = application.cacheConfig.client.get(services.route("/_public/spi/tenants")) {
        headers {
            header(HttpHeaders.Authorization, "Bearer ${application.cacheConfig.sessions.oAuthToken}")
            proxySafeHeaders(request)
            copy(HttpHeaders.XForwardedFor, request)
            copy("X-Real-Ip", request)
        }
    }

    if (request.status != HttpStatusCode.OK) {
        throw ResponseException(request, request.body())
    }

    return request.body()
}
