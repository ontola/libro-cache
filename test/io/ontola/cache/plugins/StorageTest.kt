package io.ontola.cache.plugins

import io.ktor.application.ApplicationEnvironment
import io.ktor.server.testing.withTestApplication
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.ontola.cache.util.KeyManager
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class StorageTest {
    data class StringStorageTest(
        private val environment: ApplicationEnvironment,
        val setKey: CapturingSlot<String> = slot(),
        val expireKey: CapturingSlot<String> = slot(),
        val getKey: CapturingSlot<String> = slot(),
        val value: CapturingSlot<String> = slot(),
        val expiry: CapturingSlot<Long> = slot(),
    ) {
        private val adapter = mockk<StorageAdapter<String, String>>()
        private val config = CacheConfig.fromEnvironment(environment.config, true)
        private val manager = KeyManager(config.redis)

        val storage = Storage(adapter, manager, 0L)

        init {
            coEvery { adapter.set(capture(setKey), capture(value)) } returns "OK"
            coEvery { adapter.expire(capture(expireKey), capture(expiry)) } returns true
            coEvery { adapter.get(capture(getKey)) } answers { this@StringStorageTest.value.captured }
        }
    }

    @Test
    fun storageStringCacheShouldA() {
        withTestApplication {
            runBlocking {
                val ctx = StringStorageTest(environment)
                ctx.storage.setString("a", value = "value", expiration = null)

                assertEquals("cache:a", ctx.setKey.captured)
                assertFalse(ctx.expireKey.isCaptured, "Expiry called when disabled")
                assertEquals("value", ctx.value.captured)

                val ret = ctx.storage.getString("a")

                assertEquals("cache:a", ctx.getKey.captured)
                assertEquals("value", ret)
            }
        }
    }

    @Test
    fun storageStringCacheShouldFetchExpiredKeys() {
        withTestApplication {
            runBlocking {
                val ctx = StringStorageTest(environment)

                ctx.storage.setString("a", value = "value", expiration = 0L)

                assertEquals("cache:a", ctx.setKey.captured)
                assertEquals("cache:a", ctx.expireKey.captured)
                assertEquals("value", ctx.value.captured)

                val ret = ctx.storage.getString("a")

                assertEquals("cache:a", ctx.getKey.captured)
                assertEquals("value", ret)
            }
        }
    }

    @Test
    fun storageStringCacheShouldReturnCachedValue() {
        withTestApplication {
            runBlocking {
                val ctx = StringStorageTest(environment)

                ctx.storage.setString("a", value = "value", expiration = 1L)

                assertEquals("cache:a", ctx.setKey.captured)
                assertEquals("cache:a", ctx.expireKey.captured)
                assertEquals("value", ctx.value.captured)

                val ret = ctx.storage.getString("a")

                assertFalse(ctx.getKey.isCaptured, "Adapter called while value should be cached")
                assertEquals("value", ret)
            }
        }
    }
}