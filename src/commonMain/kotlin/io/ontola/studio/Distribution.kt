package io.ontola.studio

import io.ontola.apex.webmanifest.Manifest
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * A distribution is a serveable version of a project.
 */
@Serializable
@JsExport
data class Distribution(
    val data: List<Array<String>>,
    val manifest: Manifest,
    /**
     * The sitemap, one line per resource iri
     */
    val sitemap: String,
)
