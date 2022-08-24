package tools.empathy.studio

import io.ktor.http.Url
import io.ktor.util.extension
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import tools.empathy.serialization.deep.flatten
import tools.empathy.serialization.sitemap
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile

private val logger = KotlinLogging.logger {}

internal fun readStaticDistributions(): Map<Url, Distribution> {
    val staticDistributions = Files.walk(Path("resources/sites")).map {
        if (it.isRegularFile() && it.isReadable() && it.fileName.extension == "json") {
            try {
                Json.decodeFromString<DistributionData>(it.toFile().readText())
            } catch (e: Exception) {
                logger.warn { "Could not read distribution $it" }
                null
            }
        } else {
            null
        }
    }.toList().filterNotNull().associate { data ->
        val dataSlice = data.data.flatten()
        val sitemap = dataSlice.sitemap(data.manifest.ontola.websiteIRI)

        Pair(
            data.manifest.ontola.websiteIRI,
            Distribution(
                manifest = data.manifest,
                data = dataSlice,
                meta = DistributionMeta(
                    version = "static",
                    message = "",
                    createdAt = 1,
                    live = true,
                ),
                sitemap = sitemap.urls.joinToString("\n") { it.loc },
                xmlSitemap = sitemap
            ),
        )
    }

    return staticDistributions
}
