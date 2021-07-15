package io.ontola.cache.util

import io.ktor.server.testing.withTestApplication
import io.ontola.cache.plugins.CacheConfig
import org.junit.Test
import kotlin.test.assertEquals

class KeyManagerTest {
    @Test
    fun testToKey() {
        withTestApplication {
            val config = CacheConfig.fromEnvironment(environment.config, true)
            val man = KeyManager(config)

            val key = man.toKey("https://example.com/resource/1", "en")

            assertEquals("cache:entry:https%3A//example.com/resource/1:en", key)
        }
    }

    @Test
    fun testFromKey() {
        withTestApplication {
            val config = CacheConfig.fromEnvironment(environment.config, true)
            val man = KeyManager(config)

            val (key, lang) = man.fromKey("cache:entry:https%3A//example.com/resource/1:en")

            assertEquals("https://example.com/resource/1", key)
            assertEquals("en", lang)
        }
    }
}
