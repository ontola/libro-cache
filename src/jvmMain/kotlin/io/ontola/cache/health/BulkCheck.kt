package io.ontola.cache.health

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.authority
import io.ktor.http.formUrlEncode
import io.ktor.request.header
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.tenantization.getTenants
import io.ontola.cache.util.CacheHttpHeaders

class BulkCheck : Check() {
    init {
        name = "Bulk endpoint"
    }

    override suspend fun runTest(context: PipelineContext<Unit, ApplicationCall>): Exception? {
        val tenant = context.getTenants().sites.first().location
        val origin = "http://localhost:${context.application.cacheConfig.port}"

        val response = HttpClient(CIO).post<HttpResponse>("$origin/link-lib/bulk") {
            headers {
                header(HttpHeaders.Accept, "application/hex+x-ndjson")
                header(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
                header(HttpHeaders.Cookie, context.call.request.header(HttpHeaders.Cookie))
                header(CacheHttpHeaders.WebsiteIri, tenant)
                header(HttpHeaders.XForwardedHost, tenant.authority)
                header(HttpHeaders.XForwardedProto, "https")
                header("X-Forwarded-Ssl", "on")
            }

            body = listOf(
                "resource[]" to tenant.toString()
            ).formUrlEncode()
        }

        if (response.status != HttpStatusCode.OK) {
            return Warning("Can't read tenant root from bulk (status: ${response.status})")
        }

        return null
    }
}
