package tools.empathy.libro.server.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ForwardedTest {
    @Test
    fun testForwardedHost() {
        assertEquals("example.com", "host=example.com".forwardedHost())
        assertEquals("example.com", "host=example.com;proto=https".forwardedHost())
        assertEquals("example.com", "proto=https;host=example.com".forwardedHost())
        assertEquals(null, "proto=https".forwardedHost())
    }

    @Test
    fun testForwardedProto() {
        assertEquals("https", "proto=https".forwardedProto())
        assertEquals("https", "host=http;proto=https".forwardedProto())
        assertEquals("https", "proto=https;host=http".forwardedProto())
        assertEquals(null, "host=https".forwardedProto())
    }
}
