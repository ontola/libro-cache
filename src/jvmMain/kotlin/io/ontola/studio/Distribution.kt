package io.ontola.studio

import io.ontola.apex.webmanifest.Manifest
import kotlinx.serialization.Serializable

@Serializable
data class Distribution(
    val data: List<Array<String>>,
    val manifest: Manifest,
    val sitemap: String,
)
