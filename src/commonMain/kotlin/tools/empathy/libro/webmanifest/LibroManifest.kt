@file:UseSerializers(UrlSerializer::class)

package tools.empathy.libro.webmanifest

import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import tools.empathy.url.UrlSerializer

@Serializable
data class CSPManifest(
    val defaultSrc: Set<String> = emptySet(),
    val baseUri: Set<String> = emptySet(),
    val formAction: Set<String> = emptySet(),
    val manifestSrc: Set<String> = emptySet(),
    val childSrc: Set<String> = emptySet(),
    val connectSrc: Set<String> = emptySet(),
    val fontSrc: Set<String> = emptySet(),
    val frameSrc: Set<String> = emptySet(),
    val imgSrc: Set<String> = emptySet(),
    val sandbox: Set<String> = emptySet(),
    val scriptSrc: Set<String> = emptySet(),
    val styleSrc: Set<String> = emptySet(),
    val workerSrc: Set<String> = emptySet(),
    val mediaSrc: Set<String> = emptySet(),
    val reportUri: Set<String> = emptySet(),
)

@Serializable
data class LibroManifest(
    @SerialName("allowed_external_sources")
    val allowedExternalSources: Set<String>? = null,
    @SerialName("blob_preview_iri")
    val blobPreviewIri: String = "",
    @SerialName("blob_upload_iri")
    val blobUploadIri: String = "",
    @SerialName("csp")
    val csp: CSPManifest? = null,
    @SerialName("header_background")
    val headerBackground: String = "primary",
    @SerialName("header_text")
    val headerText: String = "auto",
    @SerialName("matomo_hostname")
    val matomoHostname: String? = null,
    val preconnect: Set<String>? = setOf(
        "https://fonts.googleapis.com",
    ),
    val preload: Set<String> = emptySet(),
    @SerialName("matomo_site_id")
    val matomoSiteId: String? = null,
    @SerialName("primary_color")
    val primaryColor: String = "#475668",
    @SerialName("secondary_color")
    val secondaryColor: String = "#d96833",
    @SerialName("styled_headers")
    val styledHeaders: Boolean? = null,
    val theme: String? = null,
    @SerialName("theme_options")
    val themeOptions: String? = null,
    val tracking: List<Tracking> = emptyList(),
    @SerialName("website_iri")
    val websiteIRI: Url,
    @SerialName("websocket_path")
    val websocketPath: String? = null,
)
