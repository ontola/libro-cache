package io.ontola.cache.document

import io.ktor.http.Url
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.assets.loadAssetsManifests
import io.ontola.cache.plugins.generateCSRFToken
import io.ontola.rdf.hextuples.DataType
import io.ontola.rdf.hextuples.Hextuple
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
                assets = loadAssetsManifests(ctx.config),
            )
            val csrf = generateCSRFToken()
            val href = Url("https://mysite.local")
            val manifest = Manifest.forWebsite(href)
            val lang = "nl"
            val data = listOf(
                Hextuple(href.toString(), "http://schema.org/name", "Elefanten", DataType.Literal("http://www.w3.org/2001/XMLSchema#string"), "de", ""),
                Hextuple(href.toString(), "http://schema.org/name", "Olifanten", DataType.Literal("http://www.w3.org/2001/XMLSchema#string"), "nl", ""),
                Hextuple(href.toString(), "http://schema.org/name", "Elephants", DataType.Literal("http://www.w3.org/2001/XMLSchema#string"), "en", ""),
            ).shuffled()

            val doc = createHTML().apply {
                head {
                    renderHead(
                        href.toString(),
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
            assertContains(html, "<meta content=\"Olifanten | Libro\" property=\"og:title\">")
            assertContains(html, "<meta content=\"https://mysite.local/\" property=\"og:url\">")
            assertContains(html, "<meta name=\"csrf-token\" content=\"$csrf\">")
        }
    }
}
