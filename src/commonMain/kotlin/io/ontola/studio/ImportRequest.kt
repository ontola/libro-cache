@file:UseSerializers(UrlSerializer::class)

package io.ontola.studio

import io.ktor.http.Url
import io.ontola.apex.webmanifest.Manifest
import io.ontola.rdf.hextuples.Hextuple
import io.ontola.util.UrlSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class ImportRequest(
    val url: Url,
)
