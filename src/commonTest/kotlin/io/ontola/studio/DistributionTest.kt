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
    private val meta = DistributionMeta(
        version = "0.1",
        message = "test",
        createdAt = 1646059865021,
        live = false
    )
    private val subResources = listOf(
        SubResource(
            id = 0,
            name = "Resource 0",
            path = "/",
            type = ResourceType.RDF,
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
            graph = "http://purl.org/linked-delta/supplant",
        )
    )

    @Test
    fun `test toDistribution throws for project with empty hextuples`() {
        assertFailsWith<MalformedProjectException> {
            Project(name, iri, website, subResources, emptyList()).toDistribution(meta)
        }
    }

    @Test
    fun `test toDistribution sitemap is not empty or null`() {
        val dist = Project(name, iri, website, subResources, hextuples).toDistribution(meta)
        assertTrue(dist.sitemap.isNotEmpty())
    }

    @Test
    fun `validate toDistribution sitemap`() {
        val dist = Project(name, iri, website, subResources, hextuples).toDistribution(meta)
        assertTrue(dist.sitemap.split('\n').all { validURL(it) })
    }
}
