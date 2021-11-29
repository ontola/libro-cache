@file:UseSerializers(UrlSerializer::class)
package io.ontola.cache.tenantization

import io.ktor.http.Url
import io.ontola.cache.util.UrlSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class OntolaManifest(
    @SerialName("allowed_external_sources")
    val allowedExternalSources: Set<String>? = null,
    @SerialName("css_class")
    val cssClass: String = "default",
    @SerialName("header_background")
    val headerBackground: String = "primary",
    @SerialName("header_text")
    val headerText: String = "auto",
    @SerialName("matomo_hostname")
    val matomoHostname: String? = null,
    val preconnect: Set<String>? = null,
    val preload: Set<String> = emptySet(),
    @SerialName("matomo_site_id")
    val matomoSiteId: String? = null,
    @SerialName("primary_color")
    val primaryColor: String = "#475668",
    @SerialName("secondary_color")
    val secondaryColor: String = "#d96833",
    @SerialName("styled_headers")
    val styledHeaders: String? = null,
    val theme: String? = null,
    @SerialName("theme_options")
    val themeOptions: String? = null,
    val tracking: List<Tracking> = emptyList(),
    @SerialName("website_iri")
    val websiteIRI: Url,
    @SerialName("websocket_path")
    val websocketPath: String? = null,
)

@Serializable
data class ServiceWorker(
    val src: String = "/",
    val scope: String = if (src == "/") "/sw.js" else "$src/sw.js",
)

@Serializable
data class Icon(
    val src: String,
    val sizes: String,
    val type: String,
)

@Serializable
enum class TrackerType {
    GUA,
    GTM,
    PiwikPro,
    Matomo,
}

@Serializable
data class Tracking(
    val host: String? = null,
    val type: TrackerType,
    @SerialName("container_id")
    val containerId: String,
)

@Serializable
data class Manifest(
    @SerialName("rdf_type")
    val rdfType: String? = null,
    @SerialName("canonical_iri")
    val canonicalIri: String? = null,
    @SerialName("background_color")
    val backgroundColor: String = "#eef0f2",
    val dir: String = "rtl",
    val display: String = "standalone",
    val icons: List<Icon>? = null,
    val lang: String = "en-US",
    val name: String = "Libro",
    val ontola: OntolaManifest,
    val serviceworker: ServiceWorker = ServiceWorker(),
    @SerialName("short_name")
    val shortName: String = name,
    @SerialName("start_url")
    val startUrl: String = ensureTrailingSlash(serviceworker.scope),
    @SerialName("scope")
    val scope: String,
    @SerialName("theme_color")
    val themeColor: String = "#475668",
    @SerialName("created_at")
    val createdAt: String? = null,
) {
    companion object {
        fun forWebsite(websiteIRI: Url): Manifest = Manifest(
            ontola = OntolaManifest(
                websiteIRI = websiteIRI,
            ),
            scope = websiteIRI.encodedPath,
            serviceworker = ServiceWorker(
                scope = websiteIRI.encodedPath,
            ),
//            startUrl = Url("$websiteIRI/")
        )
    }
}
