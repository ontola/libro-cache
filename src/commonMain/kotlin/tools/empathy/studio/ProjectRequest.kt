package tools.empathy.studio

import kotlinx.serialization.Serializable
import tools.empathy.libro.webmanifest.Manifest
import tools.empathy.serialization.DataSlice

@Serializable
data class ProjectRequest(
    val data: DataSlice,
    val manifest: Manifest,
    val pages: List<String>,
    val sitemap: String,
)
