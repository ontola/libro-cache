package io.ontola.util

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals

class UrlTest {
    @Test
    fun testAppendPathEmptyWithRoot() {
        val value = Url("http://ex.com/").appendPath()
        assertEquals(Url("http://ex.com/"), value)
    }

    @Test
    fun testAppendPathEmptyWithSlashlessRoot() {
        val value = Url("http://ex.com").appendPath()
        assertEquals(Url("http://ex.com/"), value)
    }

    @Test
    fun testAppendPathEmptyWithExistingPath() {
        val value = Url("http://ex.com/b").appendPath()
        assertEquals(Url("http://ex.com/b"), value)
    }

    @Test
    fun testAppendPathSingleSegment() {
        val value = Url("http://ex.com/").appendPath("a")
        assertEquals(Url("http://ex.com/a"), value)
    }

    @Test
    fun testAppendPathMultipleSegment() {
        val value = Url("http://ex.com/").appendPath("a", "b")
        assertEquals(Url("http://ex.com/a/b"), value)
    }

    @Test
    fun testAppendPathSingleSegmentWithExistingPath() {
        val value = Url("http://ex.com/b").appendPath("a")
        assertEquals(Url("http://ex.com/b/a"), value)
    }

    @Test
    fun testAppendPathMultipleSegmentWithExistingPath() {
        val value = Url("http://ex.com/a").appendPath("b", "c")
        assertEquals(Url("http://ex.com/a/b/c"), value)
    }
}
