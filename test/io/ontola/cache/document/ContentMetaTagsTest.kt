package io.ontola.cache.document

import io.ktor.http.Url
import io.ontola.cache.tenantization.Manifest
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentMetaTagsTest {
    @Test
    fun testContentMetaTags() {
        val args = MetaDataArgs()
        val test = contentMetaTags(args)
        assertEquals(6, test.size)
    }

    @Test
    fun testRenderedMetaTags() {
        val href = Url("https://mysite.local")
        val manifest = Manifest.forWebsite(href)
        val data = ""

        val test = renderedMetaTags(href.toString(), manifest, data)

        assertEquals(
            """
            <title>undefined</title>
                      <link href="https://mysite.local" itemprop="url" rel="canonical">
                      <meta content="https://mysite.local" property="og:url">
                      <meta content="" property="og:title">
                      <meta content="" name="twitter:title">
                      <meta content="summary" name="twitter:card">
            """.trimIndent(),
            test
        )
    }
}
