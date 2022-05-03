package io.ontola.studio

import io.ontola.apex.webmanifest.Manifest
import io.ontola.empathy.web.DataSlice
import kotlinx.serialization.Serializable

@Serializable
data class ProjectRequest(
    val data: DataSlice,
    val manifest: Manifest,
    val pages: List<String>,
    val resources: List<SubResource>,
    val sitemap: String,
)
