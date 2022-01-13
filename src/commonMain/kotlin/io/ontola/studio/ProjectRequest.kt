package io.ontola.studio

import io.ontola.apex.webmanifest.Manifest
import io.ontola.rdf.hextuples.Hextuple
import kotlinx.serialization.Serializable

@Serializable
data class ProjectRequest(
    val hextuples: List<Hextuple>,
    val manifest: Manifest,
    val pages: List<String>,
    val resources: List<SubResource>,
    val sitemap: String,
)
