package tools.empathy.url

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
    fun testAppendPathSingleSegmentWithQuery() {
        val value = Url("http://ex.com/?x=y").appendPath("a")
        assertEquals(Url("http://ex.com/a?x=y"), value)
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

    @Test
    fun testRebase() {
        val value = Url("http://apex.svc.cluster.local:3000").rebase("/argu/u/session?redirect_url=https%3A%2F%2Fargu.localdev%2Fargu%2Fu%2Fsession%2Fnew%3Fredirect_url%3Dhttps%253A%252F%252Fargu.localdev%252Fargu")
        assertEquals(Url("http://apex.svc.cluster.local:3000/argu/u/session?redirect_url=https%3A%2F%2Fargu.localdev%2Fargu%2Fu%2Fsession%2Fnew%3Fredirect_url%3Dhttps%253A%252F%252Fargu.localdev%252Fargu"), value)
    }
}
