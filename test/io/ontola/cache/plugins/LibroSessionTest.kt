package io.ontola.cache.plugins

import kotlin.test.Test
import kotlin.test.assertEquals

class LibroSessionTest {
    @Test
    fun testSplitCookieWithInvalidName() {
        val header = "cookie: deviceId=56e5903e-f9b0-4005-b4e2-74a6ed146e6e; deviceId.sig=hIjhZqFNhxI7vefb8_VyybrBMMk; koa:sess=26a274ec-c9e5-4677-a993-f4a3db618e8a; koa:sess.sig=aOowMlm4ZqX8JIbjsJKDOMJFKGc"
        val expected = "26a274ec-c9e5-4677-a993-f4a3db618e8a"

        val result = splitCookieWithInvalidName(header, "koa:sess")

        assertEquals(expected, result)
    }
}
