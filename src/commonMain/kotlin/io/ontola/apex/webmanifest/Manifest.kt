package io.ontola.apex.webmanifest

import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val icons: Array<Icon>? = null,
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
