package io.ontola.cache.document

import io.ktor.http.Url
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.bundle.loadBundleManifests
import io.ontola.cache.plugins.generateCSRFToken
import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Record
import io.ontola.empathy.web.Value
import io.ontola.empathy.web.compact
import kotlinx.html.head
import kotlinx.html.stream.createHTML
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
            assertContains(html, "<meta name=\"website\" content=\"https://mysite.local\">")
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
