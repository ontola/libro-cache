package io.ontola.cache.util

import org.junit.Test
import withCacheTestApplication
import kotlin.test.assertEquals

class KeyManagerTest {
    @Test
    fun testToKey() {
        withCacheTestApplication { ctx ->
            val man = KeyManager(ctx.config.redis)

            val key = man.toEntryKey("https://example.com/resource/1", "en")

            assertEquals("cache:entry:https%3A//example.com/resource/1:en", key)
        }
    }

    @Test
    fun testFromKey() {
        withCacheTestApplication { ctx ->
            val man = KeyManager(ctx.config.redis)

            val (key, lang) = man.fromEntryKey("cache:entry:https%3A//example.com/resource/1:en")

            assertEquals("https://example.com/resource/1", key)
            assertEquals("en", lang)
        }
    }
}
