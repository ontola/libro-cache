package tools.empathy.serialization

import io.ktor.http.Url
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import tools.empathy.url.rebase
import tools.empathy.vocabularies.ActivityStreams.id

val BLACKLIST_PATTERNS = listOf(
    "<",
    "#",
    "menus/footer",
    "_:",
)

fun allowedInSitemap(id: String): Boolean = BLACKLIST_PATTERNS.none { it in id }

@Serializable
@XmlSerialName("urlset", namespace = "http://www.sitemaps.org/schemas/sitemap/0.9", prefix = "")
data class Sitemap(val urls: List<SitemapUrl>)

@Serializable
@XmlSerialName("url", namespace = "http://www.sitemaps.org/schemas/sitemap/0.9", prefix = "")
data class SitemapUrl(
    @XmlElement(true)
    val loc: String,
    @XmlElement(true)
    val alternatives: List<Alternative>? = listOf(
        Alternative(
            href = "test",
            hreflang = "nl",
        )
    ),
)

@Serializable
@XmlSerialName("link", namespace = "http://www.w3.org/1999/xhtml", prefix = "xhtml")
data class Alternative(
    val href: String,
    val hreflang: String,
    val rel: String = "alternative",
)

fun DataSlice.sitemap(baseId: Url): Sitemap = Sitemap(
    urls = keys
        .filter { key -> allowedInSitemap(key) && this[key]?.canonical().isNullOrEmpty() }
        .map { id ->
            val absolute = if (id.startsWith('/')) {
                baseId.rebase(id).toString()
            } else {
                id
            }

            SitemapUrl(
                loc = absolute,
                alternatives = this[absolute]
                    ?.translations()
                    ?.map { Alternative(it.value, it.lang) }
                    ?: emptyList(),
            )
        },
)
