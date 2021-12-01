package io.ontola.color

import kotlin.test.Test
import kotlin.test.assertEquals

class ColorTest {
    @Test
    fun testFromCss() {
        val color = Color.fromCss("#475668")

        assertEquals(71u, color.red)
        assertEquals(86u, color.green)
        assertEquals(104u, color.blue)
        assertEquals(1.0, color.alpha)
    }
}
