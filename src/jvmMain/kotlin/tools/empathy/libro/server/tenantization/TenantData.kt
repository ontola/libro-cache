package tools.empathy.libro.server.tenantization

import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.server.application.ApplicationCall
import tools.empathy.libro.server.document.PageRenderContext
import tools.empathy.libro.webmanifest.Manifest

/**
 * Context object containing information on the current tenant.
 */
sealed class TenantData(
    open val websiteIRI: Url,
    open val websiteOrigin: Url,
    open val manifest: Manifest,
) {
    data class Local(
        override val websiteIRI: Url,
        override val websiteOrigin: Url,
        override val manifest: Manifest,
        val context: suspend ApplicationCall.() -> PageRenderContext,
        val allowUnsafe: Boolean = false,
        val unsafePort: Int? = null,
    ) : TenantData(
        websiteIRI,
        websiteOrigin,
        manifest,
    ) {
        val unsafeIRI: Url
            get() = URLBuilder("http://${websiteIRI.host}${websiteIRI.fullPath}").apply {
                unsafePort?.let { port = it }
            }.build()
    }

    data class External(
        internal val client: HttpClient,
        override val websiteIRI: Url,
        override val websiteOrigin: Url,
        override val manifest: Manifest,
    ) : TenantData(
        websiteIRI,
        websiteOrigin,
        manifest,
    )
}
