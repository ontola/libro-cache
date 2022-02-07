@file:UseSerializers(UrlSerializer::class)

package io.ontola.studio

import io.ktor.http.Url
import io.ontola.apex.webmanifest.Manifest
import io.ontola.rdf.hextuples.Hextuple
import io.ontola.util.UrlSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.js.JsExport

// Keep in sync with /libro/app/modules/Studio/async/context/ProjectContext.ts
enum class ResourceType(val type: Int) {
    RDF(0),
    Manifest(1),
    Elements(2),
    MediaObject(3),
    SiteMap(4),
    Distributions(5);

    companion object {
        fun fromInt(value: Int) = ResourceType.values().first { it.type == value }
    }
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
@JsExport
data class Project(
    val name: String,
    val iri: Url,
    val websiteIRI: Url,
    val resources: List<SubResource>,
    val hextuples: List<Hextuple>,
    val manifest: Manifest = Manifest.forWebsite(websiteIRI)
)
