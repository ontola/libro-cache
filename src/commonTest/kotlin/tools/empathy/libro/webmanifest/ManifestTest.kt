package tools.empathy.libro.webmanifest

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals

class ManifestTest {
    @Test
    fun testPWA() {
        val manifest = Manifest.forWebsite(Url("https://example.com/site"))

        assertEquals("/site", manifest.scope)
        assertEquals("/site/", manifest.startUrl)
    }
}
