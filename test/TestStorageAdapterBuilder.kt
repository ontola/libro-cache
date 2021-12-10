
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.ontola.cache.bulk.CacheControl
import io.ontola.cache.bulk.CacheEntry
import io.ontola.cache.plugins.CacheConfig
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.tenantization.CachedLookupKeys
import io.ontola.cache.tenantization.Manifest
import io.ontola.cache.util.KeyManager
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.encodeToString

data class TestStorageAdapterBuilder(
    val cacheConfig: CacheConfig,
    val resources: MutableList<Triple<String, String, CacheControl>> = mutableListOf(),
    val memoizedManifests: MutableMap<String, Manifest> = mutableMapOf(),
) {
    private val keyManager = KeyManager(cacheConfig.redis)
    private val keys = mutableListOf<String>()
    private val values = mutableListOf<String>()

    fun addManifest(website: String, manifest: Manifest) {
        memoizedManifests[website] = manifest
    }

    fun build(): StorageAdapter<String, String> {
        val adapter = mockk<StorageAdapter<String, String>>()

        adapter.initGetSet()
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
        coEvery { expire("cache:WebsiteBase:https%3A//mysite.local", 600) } returns null
    }

    private fun StorageAdapter<String, String>.initGetSet() {
        coEvery { set(capture(keys), capture(values)) } returns null
        val key = slot<String>()
        coEvery { this@initGetSet.get(capture(key)) } answers {
            val i = keys.indexOf(key.captured)
            if (i == -1) {
                null
            } else {
                values[i]
            }
        }
    }

    private fun StorageAdapter<String, String>.initEmptyRedisFeatures() {
        coEvery { hmget(any(), *anyVararg()) } answers { emptyFlow() }
        coEvery { keys(any()) } returns emptyFlow()
    }
}
