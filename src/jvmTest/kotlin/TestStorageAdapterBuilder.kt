
import io.ktor.http.Url
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.bulk.CacheControl
import tools.empathy.libro.server.bulk.CacheEntry
import tools.empathy.libro.server.configuration.LibroConfig
import tools.empathy.libro.server.plugins.StorageAdapter
import tools.empathy.libro.server.sessions.oidc.OIDCServerSettings
import tools.empathy.libro.server.tenantization.CachedLookupKeys
import tools.empathy.libro.server.util.KeyManager
import tools.empathy.libro.webmanifest.Manifest

data class TestStorageAdapterBuilder(
    val libroConfig: LibroConfig,
    val resources: MutableList<Triple<String, String, CacheControl>> = mutableListOf(),
    val memoizedManifests: MutableMap<String, Manifest> = mutableMapOf(),
) {
    private val keyManager = KeyManager(libroConfig.redis)
    private val keys = mutableListOf<String>()
    private val values = mutableListOf<String>()
    private val hKeys = mutableListOf<String>()
    private val hValues = mutableListOf<MutableMap<String, String>>()

    fun addManifest(website: Url, manifest: Manifest) {
        memoizedManifests[website.toString()] = manifest
    }

    fun registerOIDCServerSettings(settings: OIDCServerSettings) {
        keys.add(keyManager.toKey("oidc", "registration", settings.origin.toString()))
        values.add(Json.encodeToString(settings))
    }

    fun addHashKey(key: String, field: String, value: String) {
        val prefixed = keyManager.toKey(key)
        if (hKeys.indexOf(prefixed) == -1) {
            hKeys.add(prefixed)
            hValues.add(mutableMapOf())
        }
        val map = hValues[hKeys.indexOf(prefixed)]

        map[field] = value
    }

    fun build(): StorageAdapter<String, String> {
        val adapter = mockk<StorageAdapter<String, String>>()

        adapter.initGetSetDel()
        adapter.initHashCommands()
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
            addHashKey(CachedLookupKeys.Manifest.name, website, libroConfig.serializer.encodeToString(manifest))
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

    private fun StorageAdapter<String, String>.initHashCommands() {
        coEvery { hset(capture(hKeys), capture(hValues)) } returns null

        val delKey = slot<String>()
        val delField0 = slot<String>()
        coEvery {
            hdel(capture(delKey), capture(delField0))
        } answers {
            val i = hKeys.indexOf(delKey.captured)

            if (i == -1) {
                null
            } else if (hValues[i].remove(delField0.captured) != null) {
                1L
            } else {
                0L
            }
        }

        val getAllKey = slot<String>()
        coEvery {
            hgetall(capture(getAllKey))
        } answers {
            val i = hKeys.indexOf(getAllKey.captured)

            if (i == -1) {
                emptyFlow()
            } else {
                hValues[i]
                    .entries
                    .asFlow()
                    .map { Pair(it.key, it.value) }
            }
        }

        val hGetKey = slot<String>()
        val hGetField = slot<String>()
        coEvery {
            hget(capture(hGetKey), capture(hGetField))
        } answers {
            val i = hKeys.indexOf(hGetKey.captured)

            if (i == -1) {
                null
            } else {
                hValues[i][hGetField.captured]
            }
        }

        val existsKey = slot<String>()
        val existsField = slot<String>()
        coEvery {
            hexists(capture(existsKey), capture(existsField))
        } answers {
            val i = hKeys.indexOf(existsKey.captured)

            if (i == -1) {
                false
            } else {
                hValues[i].containsKey(existsField.captured)
            }
        }

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
