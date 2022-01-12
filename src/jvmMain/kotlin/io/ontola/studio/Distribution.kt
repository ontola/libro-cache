package io.ontola.studio

import io.ontola.apex.webmanifest.Manifest
import io.ontola.empathy.web.DataSlice
import kotlinx.serialization.Serializable

@Serializable
data class Distribution(
    val data: DataSlice,
    val manifest: Manifest,
    val sitemap: String,
)
