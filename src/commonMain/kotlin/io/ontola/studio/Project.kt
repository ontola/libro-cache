@file:UseSerializers(UrlSerializer::class)

package io.ontola.studio

import io.ktor.http.Url
import io.ontola.util.UrlSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.js.JsExport

@Serializable
@JsExport
data class Project(
    val name: String,
    val iri: Url,
    val websiteIRI: Url,
)
