package tools.empathy.libro.server.document

import org.apache.commons.text.StringSubstitutor
import kotlin.test.Test
import kotlin.test.assertEquals

class SecurityTest {
    @Test
    fun testInterpolationVulnr() {
        val string = "\${dns:address|apache.org}"
        val processor = StringSubstitutor.createInterpolator()
        val output = processor.replace(string)

        assertEquals("\${dns:address|apache.org}", output)
    }

    @Test
    fun testInterpolation() {
        val string = "\${dns:address|apache.org}"
        val output = JsonInHtmlEscaper.translate(string)

        assertEquals("\${dns:address|apache.org}", output)
    }
}
