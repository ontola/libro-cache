package tools.empathy.libro.server.health

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.authority
import io.ktor.http.formUrlEncode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import tools.empathy.libro.server.bulk.ndEmpJson
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.tenantization.TenantData
import tools.empathy.libro.server.tenantization.getExternalTenants
import tools.empathy.libro.server.tenantization.tenant
import tools.empathy.libro.server.tenantization.tenantOrNull
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.url.appendPath

class BulkCheck : Check() {
    init {
        name = "Bulk endpoint"
    }

    override suspend fun runTest(call: ApplicationCall): Exception? {
        val tenant = if (call.tenantOrNull is TenantData.External) {
            call.tenant.websiteIRI
        } else {
            call.getExternalTenants().first().location
        }
        val internal = Url("http://localhost:${call.application.libroConfig.port}")

        val response = HttpClient(CIO).post(internal.appendPath("link-lib", "bulk")) {
            headers {
                header(HttpHeaders.Accept, ndEmpJson)
                header(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
                header(HttpHeaders.Cookie, call.request.header(HttpHeaders.Cookie))
                header(LibroHttpHeaders.WebsiteIri, tenant)
                header(HttpHeaders.XForwardedHost, tenant.authority)
                header(HttpHeaders.XForwardedProto, "https")
                header("X-Forwarded-Ssl", "on")
            }

            setBody(
                listOf(
                    "resource[]" to tenant.toString()
                ).formUrlEncode()
            )
        }

        if (response.status != HttpStatusCode.OK) {
            return Warning("Can't read tenant root from bulk (status: ${response.status})")
        }

        return null
    }
}
