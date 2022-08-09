package tools.empathy.libro.server.document

import io.ktor.http.Url
import kotlinx.html.head
import kotlinx.html.stream.createHTML
import tools.empathy.libro.server.bundle.loadBundleManifests
import tools.empathy.libro.server.plugins.generateCSRFToken
import tools.empathy.libro.webmanifest.Manifest
import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Record
import tools.empathy.serialization.Value
import tools.empathy.serialization.compact
import withCacheTestApplication
import kotlin.test.Test
import kotlin.test.assertContains

class HeadTest {
    @Test
    fun testRenderHead() {
        withCacheTestApplication { ctx ->
            val config = PageConfiguration(
                appElement = "root",
                bundles = loadBundleManifests(ctx.config),
            )
            val csrf = generateCSRFToken()
            val href = Url("https://mysite.local")
            val manifest = Manifest.forWebsite(href)
            val lang = "nl"
            val data: DataSlice = mapOf(
                href.toString() to Record(
                    href,
                    mutableMapOf(
                        "http://schema.org/name" to listOf(
                            Value.LangString("Elefanten", "de"),
                            Value.LangString("Olifanten", "nl"),
                            Value.LangString("Elephants", "en"),
                        ).shuffled()
                    )
                )
            ).compact(href)

            val doc = createHTML().apply {
                head {
                    renderHead(
                        href,
                        "nonce val",
                        csrf,
                        config,
                        manifest,
                        lang,
                        data,
                    )
                }
            }

            val html = doc.finalize()
            assertContains(html, "<title>Olifanten</title>")
            assertContains(html, "<meta name=\"website\" content=\"https://mysite.local/\">")
            assertContains(html, "<link href=\"/manifest.json\" rel=\"manifest\">")
            assertContains(html, "<meta content=\"Olifanten | Libro\" property=\"og:title\">")
            assertContains(html, "<meta content=\"https://mysite.local/\" property=\"og:url\">")
            assertContains(html, "<meta name=\"csrf-token\" content=\"$csrf\">")
        }
    }

    @Test
    fun testRenderHeadSubPath() {
        withCacheTestApplication { ctx ->
            val config = PageConfiguration(
                appElement = "root",
                bundles = loadBundleManifests(ctx.config),
            )
            val csrf = generateCSRFToken()
            val href = Url("https://mysite.local/site")
            val manifest = Manifest.forWebsite(href)
            val lang = "nl"
            val data: DataSlice = mapOf(
                href.toString() to Record(
                    href,
                    mutableMapOf(
                        "http://schema.org/name" to listOf(
                            Value.LangString("Elefanten", "de"),
                            Value.LangString("Olifanten", "nl"),
                            Value.LangString("Elephants", "en"),
                        ).shuffled()
                    )
                )
            ).compact(href)

            val doc = createHTML().apply {
                head {
                    renderHead(
                        href,
                        "nonce val",
                        csrf,
                        config,
                        manifest,
                        lang,
                        data,
                    )
                }
            }

            val html = doc.finalize()
            assertContains(html, "<title>Olifanten</title>")
            assertContains(html, "<meta name=\"website\" content=\"https://mysite.local/site\">")
            assertContains(html, "<link href=\"/site/manifest.json\" rel=\"manifest\">")
            assertContains(html, "<meta content=\"Olifanten | Libro\" property=\"og:title\">")
            assertContains(html, "<meta content=\"https://mysite.local/site\" property=\"og:url\">")
            assertContains(html, "<meta name=\"csrf-token\" content=\"$csrf\">")
        }
    }
}
