package tools.empathy.studio

import kotlinx.serialization.Serializable
import tools.empathy.libro.webmanifest.Manifest
import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Sitemap
import tools.empathy.serialization.allowedInSitemap
import tools.empathy.serialization.sitemap
import tools.empathy.serialization.splitMultilingual

@Serializable
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
data class Distribution(
    val meta: DistributionMeta,
    val data: DataSlice,
    val manifest: Manifest,
    /**
     * The sitemap, one line per resource iri
     */
    val sitemap: String,
    val xmlSitemap: Sitemap = data.sitemap(),
)

fun Project.toDistribution(meta: DistributionMeta): Distribution {
    if (data.isEmpty()) {
        throw MalformedProjectException("Project didn't contain any records.")
    }

    val sitemap = data.keys.filter(::allowedInSitemap)
    if (sitemap.isEmpty()) {
        throw MalformedProjectException("Project didn't contain any records valid for the sitemap.")
    }

    return Distribution(
        data = data.splitMultilingual(websiteIRI),
        manifest = manifest,
        sitemap = sitemap.joinToString(separator = "\n"),
        meta = meta,
    )
}
