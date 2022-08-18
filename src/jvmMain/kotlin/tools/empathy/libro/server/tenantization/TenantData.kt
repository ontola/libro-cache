package tools.empathy.libro.server.tenantization

import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.fullPath
import tools.empathy.libro.webmanifest.Manifest

/**
 * Context object containing information on the current tenant.
 */
data class TenantData(
    internal val client: HttpClient,
    val websiteIRI: Url,
    val websiteOrigin: Url,
    val manifest: Manifest,
    val allowUnsafe: Boolean = false,
    val unsafePort: Int? = null,
) {
    val unsafeIRI: Url
        get() = URLBuilder("http://${websiteIRI.host}${websiteIRI.fullPath}").apply {
            unsafePort?.let { port = it }
        }.build()
}
