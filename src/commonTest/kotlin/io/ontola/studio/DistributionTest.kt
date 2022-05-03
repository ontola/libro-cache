package io.ontola.studio

import io.ktor.http.Url
import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Record
import io.ontola.empathy.web.Value
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
    private val slice: DataSlice = mapOf(
        "https://example.com/" to Record(
            Url("https://example.com/"),
            fields = mutableMapOf(
                "name" to listOf(Value.Str("test"))
            )
        )
    )

    @Test
    fun `toDistribution throws for project without records`() {
        assertFailsWith<MalformedProjectException> {
            Project(name, iri, website, subResources, emptyMap()).toDistribution(meta)
        }
    }

    @Test
    fun `toDistribution sitemap is not empty or null`() {
        val dist = Project(name, iri, website, subResources, slice).toDistribution(meta)
        assertTrue(dist.sitemap.isNotEmpty())
    }

    @Test
    fun `toDistribution creates a valid sitemap`() {
        val dist = Project(name, iri, website, subResources, slice).toDistribution(meta)
        assertTrue(dist.sitemap.split('\n').all { validURL(it) })
    }
}
