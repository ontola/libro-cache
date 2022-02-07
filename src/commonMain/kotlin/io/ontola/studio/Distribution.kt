package io.ontola.studio

import io.ontola.apex.webmanifest.Manifest
import io.ontola.rdf.hextuples.Hextuple
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

/**
 * A distribution is a serveable version of a project.
 */
@Serializable
@JsExport
data class Distribution(
    val data: List<Hextuple>,
    val manifest: Manifest,
    /**
     * The sitemap, one line per resource iri
     */
    val sitemap: String,
)

val blackList = listOf<String>("#", "_", "menus/footer")

fun allowedString(hextuple: String): Boolean {
    for (substring in blackList) {
        if (substring in hextuple) {
            return false
        }
    }
    return true
}

fun Project.toDistribution(): Distribution {
    if (hextuples.isEmpty()) {
        throw MalformedProjectException("Project didn't contain any hextuples.")
    }

    val sitemap = HashSet<String>()
    for (hextuple in hextuples) {
        if (allowedString(hextuple.subject)) {
            sitemap.add(hextuple.subject)
        }
    }
    if (sitemap.isEmpty()) {
        throw MalformedProjectException("Sitemap is empty")
    }

    return Distribution(
        data = this.hextuples,
        manifest = this.manifest,
        sitemap = sitemap.joinToString(separator = "\n"),
    )
}
