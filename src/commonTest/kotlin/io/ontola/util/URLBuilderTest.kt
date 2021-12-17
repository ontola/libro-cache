package io.ontola.util

import io.ktor.http.URLBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class URLBuilderTest {
    @Test
    fun testAppendPathEmptyWithRoot() {
        val value = URLBuilder("http://ex.com/").apply { appendPath() }.buildString()
        assertEquals("http://ex.com/", value)
    }

    @Test
    fun testAppendPathEmptyWithSlashlessRoot() {
        val value = URLBuilder("http://ex.com").apply { appendPath() }.buildString()
        assertEquals("http://ex.com/", value)
    }

    @Test
    fun testAppendPathEmptyWithExistingPath() {
        val value = URLBuilder("http://ex.com/b").apply { appendPath() }.buildString()
        assertEquals("http://ex.com/b", value)
    }

    @Test
    fun testAppendPathSingleSegment() {
        val value = URLBuilder("http://ex.com/").apply { appendPath("a") }.buildString()
        assertEquals("http://ex.com/a", value)
    }

    @Test
    fun testAppendPathSingleSegmentWithExistingPath() {
        val value = URLBuilder("http://ex.com/b").apply { appendPath("a") }.buildString()
        assertEquals("http://ex.com/b/a", value)
    }
}
