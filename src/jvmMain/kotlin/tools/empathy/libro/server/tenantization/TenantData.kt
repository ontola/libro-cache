package tools.empathy.libro.server.tenantization

import io.ktor.client.HttpClient
import io.ktor.http.Url
import tools.empathy.libro.webmanifest.Manifest

/**
 * Context object containing information on the current tenant.
 */
data class TenantData(
    internal val client: HttpClient,
    val websiteIRI: Url,
    val websiteOrigin: Url,
    val manifest: Manifest,
)
