
import io.ktor.http.Url
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.bulk.CacheControl
import io.ontola.cache.bulk.CacheEntry
import io.ontola.cache.plugins.CacheConfig
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.tenantization.CachedLookupKeys
import io.ontola.cache.util.KeyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

data class TestStorageAdapterBuilder(
    val cacheConfig: CacheConfig,
    val resources: MutableList<Triple<String, String, CacheControl>> = mutableListOf(),
    val memoizedManifests: MutableMap<String, Manifest> = mutableMapOf(),
) {
    private val keyManager = KeyManager(cacheConfig.redis)
    private val keys = mutableListOf<String>()
    private val values = mutableListOf<String>()
    private val hKeys = mutableListOf<String>()
    private val hValues = mutableListOf<Map<String, String>>()

    fun addManifest(website: Url, manifest: Manifest) {
        memoizedManifests[website.toString()] = manifest
    }

    fun build(): StorageAdapter<String, String> {
        val adapter = mockk<StorageAdapter<String, String>>()

        adapter.initGetSetDel()
        adapter.initHmGetHSet()
        adapter.initEmptyRedisFeatures()
        adapter.initCacheEntries()
        initManifests()
        adapter.initWebsiteBaseMemoization()

        return adapter
    }

    private fun StorageAdapter<String, String>.initCacheEntries() {
        resources.forEach { resource ->
            every { hmget("cache:entry:${resource.first.replace(":", "%3A")}:en", *CacheEntry.fields) } answers {
                val data = mapOf(
                    "iri" to resource.first,
                    "status" to "200",
                    "cacheControl" to resource.third.toString(),
                    "contents" to resource.second,
                )
                val res = CacheEntry.fields.toList().map { Pair(it, data[it]) }
                flowOf(*res.toTypedArray())
            }
        }
    }

    private fun initManifests() {
        for ((website, manifest) in memoizedManifests) {
            keys.add(keyManager.toKey(CachedLookupKeys.Manifest.name, website))
            values.add(cacheConfig.serializer.encodeToString(manifest))
        }
    }

    private fun StorageAdapter<String, String>.initWebsiteBaseMemoization() {
        coEvery { expire(any(), 600) } returns null
    }

    private fun StorageAdapter<String, String>.initGetSetDel() {
        coEvery { set(capture(keys), capture(values)) } returns null

        val key = slot<String>()
        coEvery { this@initGetSetDel.get(capture(key)) } answers {
            val i = keys.indexOf(key.captured)
            if (i == -1) {
                null
            } else {
                values[i]
            }
        }

        coEvery { del(capture(key)) } returns null
    }

    private fun StorageAdapter<String, String>.initHmGetHSet() {
        coEvery { hset(capture(hKeys), capture(hValues)) } returns null

        fun hmgetImpl(key: String, fields: List<String>): Flow<Pair<String, String>> {
            val i = hKeys.indexOf(key)
            return if (i == -1) {
                emptyFlow()
            } else {
                hValues[i]
                    .entries
                    .asFlow()
                    .filter { fields.contains(it.key) }
                    .map { Pair(it.key, it.value) }
            }
        }

        val key = slot<String>()
        val hmkey0 = slot<String>()
        val hmkey1 = slot<String>()
        val hmkey2 = slot<String>()
        val hmkey3 = slot<String>()
        val hmkey4 = slot<String>()
        val hmkey5 = slot<String>()
        val hmkey6 = slot<String>()
        val hmkey7 = slot<String>()

        coEvery {
            hmget(capture(key), capture(hmkey0))
        } answers {
            hmgetImpl(key.captured, listOf(hmkey0.captured))
        }
        coEvery {
            hmget(capture(key), capture(hmkey0), capture(hmkey1))
        } answers {
            hmgetImpl(key.captured, listOf(hmkey0.captured, hmkey1.captured))
        }
        coEvery {
            hmget(capture(key), capture(hmkey0), capture(hmkey1), capture(hmkey2))
        } answers {
            hmgetImpl(key.captured, listOf(hmkey0.captured, hmkey1.captured, hmkey2.captured))
        }
        coEvery {
            hmget(capture(key), capture(hmkey0), capture(hmkey1), capture(hmkey2), capture(hmkey3))
        } answers {
            hmgetImpl(key.captured, listOf(hmkey0.captured, hmkey1.captured, hmkey2.captured, hmkey3.captured))
        }
        coEvery {
            hmget(capture(key), capture(hmkey0), capture(hmkey1), capture(hmkey2), capture(hmkey3), capture(hmkey4))
        } answers {
            hmgetImpl(key.captured, listOf(hmkey0.captured, hmkey1.captured, hmkey2.captured, hmkey3.captured, hmkey4.captured))
        }
        coEvery {
            hmget(capture(key), capture(hmkey0), capture(hmkey1), capture(hmkey2), capture(hmkey3), capture(hmkey4), capture(hmkey5))
        } answers {
            hmgetImpl(key.captured, listOf(hmkey0.captured, hmkey1.captured, hmkey2.captured, hmkey3.captured, hmkey4.captured, hmkey5.captured))
        }
        coEvery {
            hmget(capture(key), capture(hmkey0), capture(hmkey1), capture(hmkey2), capture(hmkey3), capture(hmkey4), capture(hmkey5), capture(hmkey6))
        } answers {
            hmgetImpl(key.captured, listOf(hmkey0.captured, hmkey1.captured, hmkey2.captured, hmkey3.captured, hmkey4.captured, hmkey5.captured, hmkey6.captured))
        }
        coEvery {
            hmget(capture(key), capture(hmkey0), capture(hmkey1), capture(hmkey2), capture(hmkey3), capture(hmkey4), capture(hmkey5), capture(hmkey6), capture(hmkey7))
        } answers {
            hmgetImpl(key.captured, listOf(hmkey0.captured, hmkey1.captured, hmkey2.captured, hmkey3.captured, hmkey4.captured, hmkey5.captured, hmkey6.captured, hmkey7.captured))
        }
    }

    private fun StorageAdapter<String, String>.initEmptyRedisFeatures() {
        coEvery { hmget(any(), *anyVararg()) } answers { emptyFlow() }
        coEvery { keys(any()) } returns emptyFlow()
    }
}
