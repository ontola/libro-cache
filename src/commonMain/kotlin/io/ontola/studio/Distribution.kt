package io.ontola.studio

import io.ontola.apex.webmanifest.Manifest
import io.ontola.empathy.web.DataSlice
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

val blackList = listOf<String>("#", "_", "menus/footer")

fun allowedInSitemap(hextuple: String): Boolean {
    for (substring in blackList) {
        if (substring in hextuple) {
            return false
        }
    }
    return true
}

fun Project.toDistribution(meta: DistributionMeta): Distribution {
    if (data.isEmpty()) {
        throw MalformedProjectException("Project didn't contain any records.")
    }

    val sitemap = data.keys.filter(::allowedInSitemap)
    if (sitemap.isEmpty()) {
        throw MalformedProjectException("Project didn't contain any records valid for the sitemap.")
    }

    return Distribution(
        data = data,
        manifest = manifest,
        sitemap = sitemap.joinToString(separator = "\n"),
        meta = meta,
    )
}
