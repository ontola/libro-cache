package tools.empathy.libro.server.plugins

import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.withTestApplication
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import tools.empathy.libro.server.util.KeyManager
import withCacheTestApplication
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

    private val env = createTestEnvironment {
        (this.config as MapApplicationConfig).apply {
            put("ktor.deployment.port", "3080")
            put("studio.domain", "test.rdf.studio")
            put("studio.skipAuth", "false")
        }
    }

    @Test
    fun storageStringShouldA() {
        withCacheTestApplication {
            runBlocking {
                val ctx = StringStorageTest(env)
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
    fun storageShouldFetchKeys() {
        withTestApplication {
            runBlocking {
                val ctx = StringStorageTest(env)

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
}
