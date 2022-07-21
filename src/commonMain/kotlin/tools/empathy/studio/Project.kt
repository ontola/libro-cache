@file:UseSerializers(UrlSerializer::class)

package tools.empathy.studio

import io.ktor.http.Url
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import tools.empathy.libro.webmanifest.Manifest
import tools.empathy.serialization.DataSlice
import tools.empathy.url.UrlSerializer

// Keep in sync with /libro/app/modules/Studio/async/lib/types.ts
@Serializable
enum class ResourceType {
    RDF,
    Manifest,
    Elements,
    MediaObject,
    SiteMap,
    Distributions,
}

@Serializable
data class SubResource(
    val id: Int,
    val name: String,
    val path: String,
    val type: ResourceType,
    val value: String,
)

@Serializable
data class Project(
    val name: String,
    val iri: Url,
    val websiteIRI: Url,
    val data: DataSlice,
    val manifest: Manifest = Manifest.forWebsite(websiteIRI)
)
