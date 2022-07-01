package io.ontola.empathy.web

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DataSliceTest {
    @Test
    fun `localised rebases root website`() {
        val site = Url("https://example.com/")
        val page = Value.Id.Global("https://example.com/about")
        val lang = "en"

        val actual = page.localised(site, lang)

        assertEquals(Url("https://example.com/en/about"), actual)
    }

    @Test
    fun `localised rebases nested website`() {
        val site = Url("https://example.com/info")
        val page = Value.Id.Global("https://example.com/info/about")
        val lang = "en"

        val actual = page.localised(site, lang)

        assertEquals(Url("https://example.com/info/en/about"), actual)
    }

    @Test
    fun `localised replaced path name`() {
        val site = Url("https://example.com/info")
        val page = Value.Id.Global("https://example.com/info/about")
        val lang = "nl"

        val actual = page.localised(site, lang, "over")

        assertEquals(Url("https://example.com/info/nl/over"), actual)
    }

    @Test
    fun `splitMultilingual divides a record`() {
        val website = Url("https://mysite.local/info")
        val about = Url("https://mysite.local/info/about")
        val aboutNl = Url("https://mysite.local/info/nl/over")
        val aboutEn = Url("https://mysite.local/info/en/about")

        val data: DataSlice = mapOf(
            about.toString() to Record(
                about,
                mutableMapOf(
                    "_ids" to listOf(
                        Value.LangString("over", "nl"),
                        Value.LangString("about", "en"),
                    ),
                    "http://schema.org/name" to listOf(
                        Value.LangString("Olifanten", "nl"),
                        Value.LangString("Elephants", "en"),
                    ).shuffled()
                )
            )
        )

        val output = data.splitMultilingual(website)

        assertEquals(3, output.size)

        val sourceDoc = output[about.toString()]
        assertNotNull(sourceDoc)
        assertEquals(
            sourceDoc.translations(),
            listOf(
                Value.LangString(aboutNl.toString(), "nl"),
                Value.LangString(aboutEn.toString(), "en"),
            )
        )

        val enDoc = output[aboutEn.toString()]
        assertNotNull(enDoc)
        assertEquals(about.toValue(), enDoc.canonical()!!.first())

        val nlDoc = output[aboutNl.toString()]
        assertNotNull(nlDoc)
        assertEquals(about.toValue(), nlDoc.canonical()!!.first())
    }

    @Test
    fun `splitMultilingual updates internal links`() {
        val website = Url("https://mysite.local/info")
        val homeNl = Url("https://mysite.local/info/nl")
        val homeEn = Url("https://mysite.local/info/en")
        val about = Url("https://mysite.local/info/about")
        val aboutNl = Url("https://mysite.local/info/nl/over")
        val aboutEn = Url("https://mysite.local/info/en/about")

        val data: DataSlice = mapOf(
            website.toString() to Record(
                website,
                mutableMapOf(
                    "_ids" to listOf(
                        Value.LangString("", "nl"),
                        Value.LangString("", "en"),
                    ),
                    "about" to listOf(Value.Id.Global(about))
                )
            ),
            about.toString() to Record(
                about,
                mutableMapOf(
                    "_ids" to listOf(
                        Value.LangString("over", "nl"),
                        Value.LangString("about", "en"),
                    ),
                    "home" to listOf(Value.Id.Global(website))
                )
            )
        )

        val output = data.splitMultilingual(website)

        val enHome = output[homeEn.toString()]
        assertNotNull(enHome)
        assertEquals(Value.Id.Global(aboutEn), enHome["about"]!!.first())

        val nlHome = output[homeNl.toString()]
        assertNotNull(nlHome)
        assertEquals(Value.Id.Global(aboutNl), nlHome["about"]!!.first())

        val enDoc = output[aboutEn.toString()]
        assertNotNull(enDoc)
        assertEquals(Value.Id.Global(homeEn), enDoc["home"]!!.first())

        val nlDoc = output[aboutNl.toString()]
        assertNotNull(nlDoc)
        assertEquals(Value.Id.Global(homeNl), nlDoc["home"]!!.first())
    }

    @Test
    fun `splitMultilingual handles nested resources`() {
        val website = Url("https://mysite.local/info")
        val about = Url("https://mysite.local/info/about")
        val aboutEnText = Url("https://mysite.local/info/about.<http://schema.org/text>.0")
        val aboutEnTextValue = Url("https://argu.localdev/info/participatie.<http://schema.org/text>.0.<https://ns.ontola.io/libro/value>")
        val aboutNlText = Url("https://mysite.local/info/about.<http://schema.org/text>.1")
        val aboutNlTextValue = Url("https://argu.localdev/info/participatie.<http://schema.org/text>.1.<https://ns.ontola.io/libro/value>")
        val aboutNl = Url("https://mysite.local/info/nl/over")
        val aboutEn = Url("https://mysite.local/info/en/about")

        val data: DataSlice = mapOf(
            about.toString() to Record(
                about,
                mutableMapOf(
                    "_ids" to listOf(
                        Value.LangString("over", "nl"),
                        Value.LangString("about", "en"),
                    ),
                    "http://schema.org/text" to listOf(
                        Value.Id.Global(aboutEnText),
                        Value.Id.Global(aboutNlText),
                    ).shuffled()
                )
            ),
            aboutEnText.toString() to Record(
                aboutEnText,
                mutableMapOf(
                    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" to listOf(Value.Id.Global("https://ns.ontola.io/libro/TranslatedObject")),
                    "https://ns.ontola.io/libro/language" to listOf(Value.Str("en")),
                    "https://ns.ontola.io/libro/value" to listOf(Value.Id.Global(aboutEnTextValue)),
                )
            ),
            aboutNlText.toString() to Record(
                aboutNlText,
                mutableMapOf(
                    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" to listOf(Value.Id.Global("https://ns.ontola.io/libro/TranslatedObject")),
                    "https://ns.ontola.io/libro/language" to listOf(Value.Str("nl")),
                    "https://ns.ontola.io/libro/value" to listOf(Value.Id.Global(aboutNlTextValue.toString())),
                )
            ),
            aboutEnTextValue.toString() to Record(
                aboutNlText,
                mutableMapOf(
                    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" to listOf(Value.Id.Global("https://ns.ontola.io/elements/Document")),
                )
            ),
            aboutNlTextValue.toString() to Record(
                aboutNlText,
                mutableMapOf(
                    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" to listOf(Value.Id.Global("https://ns.ontola.io/elements/Document")),
                )
            ),
        )

        val output = data.splitMultilingual(website)

        val sourceDoc = output[about.toString()]
        assertNotNull(sourceDoc)
        assertEquals(
            sourceDoc.translations(),
            listOf(
                Value.LangString(aboutNl.toString(), "nl"),
                Value.LangString(aboutEn.toString(), "en"),
            )
        )

        val enDoc = output[aboutEn.toString()]
        assertNotNull(enDoc)
        assertEquals(enDoc.canonical()!!.first(), about.toValue())

        val nlDoc = output[aboutNl.toString()]
        assertNotNull(nlDoc)
        assertEquals(nlDoc.canonical()!!.first(), about.toValue())
    }
}
