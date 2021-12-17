@file:UseSerializers(UrlSerializer::class)

package io.ontola.apex.webmanifest

import io.ktor.http.Url
import io.ontola.util.UrlSerializer
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
