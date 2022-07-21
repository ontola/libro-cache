package tools.empathy.serialization

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

val BLACKLIST_PATTERNS = listOf(
    "<",
    "#",
    "menus/footer",
    "_:",
)

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

fun DataSlice.sitemap(): Sitemap = Sitemap(
    urls = keys
        .filter { key -> BLACKLIST_PATTERNS.none { key.contains(it) } && this[key]?.canonical().isNullOrEmpty() }
        .map { id ->
            SitemapUrl(
                loc = id,
                alternatives = this[id]
                    ?.translations()
                    ?.map { Alternative(it.value, it.lang) }
                    ?: emptyList(),
            )
        },
)
