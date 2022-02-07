package io.ontola.studio

import io.ktor.http.Url
import io.ontola.rdf.hextuples.DataType
import io.ontola.rdf.hextuples.Hextuple
import io.ontola.util.validURL
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DistributionTest {
    private val name = "Example site"
    private val iri = Url("https:/example.com/")
    private val website = Url("https:/example.com/")
    private val subResources = listOf<SubResource>(
        SubResource(
            id = 0,
            name = "Resource 0",
            path = "/",
            type = ResourceType.fromInt(0),
            value = "todo",
        )
    )
    private val hextuples = listOf<Hextuple>(
        Hextuple(
            subject = "https://example.com/",
            predicate = "",
            value = "",
            datatype = DataType.fromValue(""),
            language = "",
            graph = "",
        )
    )

    @Test
    fun `test toDistribution throws for project with empty hextuples`() {
        assertFailsWith<MalformedProjectException> {
            Project(name, iri, website, subResources, emptyList()).toDistribution()
        }
    }

    @Test
    fun `test toDistribution sitemap is not empty or null`() {
        val dist = Project(name, iri, website, subResources, hextuples).toDistribution()
        assertTrue(dist.sitemap.isNotEmpty())
    }

    @Test
    fun `validate toDistribution sitemap`() {
        val dist = Project(name, iri, website, subResources, hextuples).toDistribution()
        assertTrue(dist.sitemap.split('\n').all { validURL(it) })
    }
}
