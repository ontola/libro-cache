package io.ontola.cache.features

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.FlushMode
import io.lettuce.core.KeyValue
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.ontola.cache.bulk.CacheControl
import io.ontola.cache.bulk.CacheEntry
import io.ontola.cache.util.KeyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold

interface StorageAdapter<K : Any, V : Any> {
    suspend fun expire(key: K, seconds: Long): Boolean?

    suspend fun get(key: K): V?

    fun hmget(key: K, vararg fields: K): Flow<KeyValue<K, V>>

    suspend fun hset(key: K, map: Map<K, V>): Long?

    suspend fun flushdbAsync(): String?

    suspend fun set(key: K, value: V): String?
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisAdapter(val client: RedisCoroutinesCommands<String, String>) : StorageAdapter<String, String> {
    override suspend fun expire(key: String, seconds: Long): Boolean? {
        return expire(key, seconds)
    }

    override fun hmget(key: String, vararg fields: String): Flow<KeyValue<String, String>> {
        return client.hmget(key, *fields)
    }

    override suspend fun hset(key: String, map: Map<String, String>): Long? {
        return client.hset(key, map)
    }

    override suspend fun get(key: String): String? {
        return client.get(key)
    }

    override suspend fun flushdbAsync(): String? {
        return client.flushdb(FlushMode.ASYNC)
    }

    override suspend fun set(key: String, value: String): String? {
        return client.set(key, value)
    }
}

class Storage(
    private val adapter: StorageAdapter<String, String>,
    private val keyManager: KeyManager,
    private val expiration: Long?
) {
    class Configuration {
        lateinit var adapter: StorageAdapter<String, String>
        lateinit var keyManager: KeyManager
        var expiration: Long? = null
    }

    suspend fun clear(): String? = adapter.flushdbAsync()

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun getCacheEntry(iri: String, lang: String): CacheEntry? {
        val key = keyManager.toKey(iri, lang)

        val hash = adapter
            .hmget(key, "iri", "status", "cacheControl", "contents")
            .fold<KeyValue<String, String>, MutableMap<String, String>>(mutableMapOf()) { e, h ->
                h.ifHasValue { e[h.key] = it }
                e
            }

        if (hash.isEmpty()) {
            return null
        }

        return CacheEntry(
            iri = hash["iri"]!!,
            status = HttpStatusCode.fromValue(hash["status"]!!.toInt()),
            cacheControl = CacheControl.valueOf(hash["cacheControl"]!!),
            contents = hash["contents"],
        )
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun setCacheEntries(entries: List<CacheEntry>, lang: String) {
        entries
            .associateBy(
                { keyManager.toKey(it.iri, lang) },
                {
                    mapOf(
                        "iri" to it.iri,
                        "status" to it.status.value.toString(10),
                        "cacheControl" to it.cacheControl.toString(),
                        "contents" to it.contents.orEmpty(),
                    )
                }
            )
            .forEach {
                val key = keyManager.toKey(it.key, lang)
                adapter.hset(key, it.value)
                expiration?.let { cacheExpiration ->
                    adapter.expire(key, cacheExpiration)
                }
            }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, Storage> {
        override val key = AttributeKey<Storage>("Storage")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Storage {
            val config = Configuration()
                .apply {
                    keyManager = KeyManager(pipeline.cacheConfig)
                }.apply(configure)

            val feature = Storage(
                config.adapter,
                config.keyManager,
                config.expiration,
            )
            pipeline.attributes.put(StorageKey, feature)

            return feature
        }
    }
}

private val StorageKey = AttributeKey<Storage>("StorageKey")

internal val ApplicationCallPipeline.storage: Storage
    get() = attributes.getOrNull(StorageKey) ?: reportMissingRegistry()

private fun reportMissingRegistry(): Nothing {
    throw StorageNotYetConfiguredException()
}
class StorageNotYetConfiguredException :
    IllegalStateException("Logger is not yet ready: you are asking it to early before the Storage feature.")
