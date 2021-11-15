
import io.ktor.http.Url
import io.lettuce.core.RedisURI
import io.mockk.coEvery
import io.mockk.mockk
import io.ontola.cache.plugins.CachedLookupKeys
import io.ontola.cache.plugins.Manifest
import io.ontola.cache.plugins.RedisConfig
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.util.KeyManager
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val defaultConfig = RedisConfig(
    RedisURI.create("redis://invalid.test"),
    invalidationChannel = "",
    invalidationGroup = "",
)
val testKeyManager = KeyManager(defaultConfig)

fun blankStorage(): StorageAdapter<String, String> {
    val storage = mockk<StorageAdapter<String, String>>()

    coEvery {
        storage.hmget(any(), any(), any(), any(), any())
    } answers {
        emptyFlow()
    }

    return storage
}

suspend fun StorageAdapter<String, String>.setWebsiteBase(uri: Url, websiteBase: Url) {
    val key = testKeyManager.toKey(CachedLookupKeys.WebsiteBase.name, uri.toString())
    val storage = this

    coEvery {
        storage.get(key)
    } answers {
        websiteBase.toString()
    }
}

@OptIn(ExperimentalSerializationApi::class)
suspend fun StorageAdapter<String, String>.setManifest(websiteBase: Url, manifest: Manifest) {
    val key = testKeyManager.toKey(CachedLookupKeys.Manifest.name, websiteBase.toString())
    val storage = this

    coEvery {
        storage.get(key)
    }.answers {
        Json.encodeToString(manifest)
    }
}
