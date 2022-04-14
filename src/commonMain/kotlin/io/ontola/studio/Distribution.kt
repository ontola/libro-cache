package io.ontola.studio

import io.ontola.apex.webmanifest.Manifest
import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.toSlice
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
@JsExport
data class DistributionMeta(
    val version: String,
    val message: String,
    val createdAt: Long,
    val live: Boolean,
)

/**
 * A distribution is a serveable version of a project.
 */
@Serializable
@JsExport
data class Distribution(
    val meta: DistributionMeta,
    val data: DataSlice,
    val manifest: Manifest,
    /**
     * The sitemap, one line per resource iri
     */
    val sitemap: String,
)

val blackList = listOf("#", "_", "menus/footer")

fun allowedString(hextuple: String): Boolean {
    for (substring in blackList) {
        if (substring in hextuple) {
            return false
        }
    }
    return true
}

fun Project.toDistribution(meta: DistributionMeta): Distribution {
    if (hextuples.isEmpty()) {
        throw MalformedProjectException("Project didn't contain any hextuples.")
    }

    val sitemap = HashSet<String>()
    for (subject in data.keys) {
        if (allowedString(subject)) {
            sitemap.add(subject)
        }
    }
    if (sitemap.isEmpty()) {
        throw MalformedProjectException("Sitemap is empty")
    }

    return Distribution(
        data = data,
        manifest = manifest,
        sitemap = sitemap.joinToString(separator = "\n"),
        meta = meta,
    )
}
