package io.ontola.studio

import io.ontola.apex.webmanifest.Manifest
import io.ontola.empathy.web.DataSlice
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * A distribution is a serveable version of a project.
 */
@Serializable
@JsExport
data class Distribution(
    val data: DataSlice,
    val manifest: Manifest,
    /**
     * The sitemap, one line per resource iri
     */
    val sitemap: String,
)
