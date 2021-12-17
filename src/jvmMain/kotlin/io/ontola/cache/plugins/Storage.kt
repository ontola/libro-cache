package io.ontola.cache.plugins

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.FlushMode
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.ontola.cache.bulk.CacheControl
import io.ontola.cache.bulk.CacheEntry
import io.ontola.cache.util.KeyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

interface StorageAdapter<K : Any, V : Any> {
    suspend fun del(key: K): Long?

    suspend fun expire(key: K, seconds: Long): Boolean?

    suspend fun get(key: K): V?

    suspend fun hget(key: String, field: String): String?

    fun hmget(key: K, vararg fields: K): Flow<Pair<K, V?>>

    suspend fun hset(key: K, map: Map<K, V>): Long?

    suspend fun flushdbAsync(): String?

    suspend fun keys(pattern: K): Flow<String>

    suspend fun set(key: K, value: V): String?
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisAdapter(val client: RedisCoroutinesCommands<String, String>) : StorageAdapter<String, String> {
    override suspend fun del(key: String): Long? {
        return client.del(key)
    }

    override suspend fun expire(key: String, seconds: Long): Boolean? {
        return client.expire(key, seconds)
    }

    override suspend fun hget(key: String, field: String): String? {
        return client.hget(key, field)
    }

    override fun hmget(key: String, vararg fields: String): Flow<Pair<String, String?>> {
        return client.hmget(key, *fields).map { Pair(it.key, it.getValueOrElse(null)) }
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

    override suspend fun keys(pattern: String): Flow<String> {
        return client.keys(pattern)
    }

    override suspend fun set(key: String, value: String): String? {
        return client.set(key, value)
    }
}

typealias TempString = Pair<Long, String>

private val logger = KotlinLogging.logger {}

class Storage(
    private val adapter: StorageAdapter<String, String>,
    private val keyManager: KeyManager,
    private val expiration: Long?,
) {
    private val strings: MutableMap<String, TempString> = mutableMapOf()

    class Configuration {
        lateinit var adapter: StorageAdapter<String, String>
        lateinit var persistentAdapter: StorageAdapter<String, String>
        lateinit var keyManager: KeyManager
        var expiration: Long? = null
    }

    suspend fun clear(): String? {
        logger.warn { "Executing cache clear" }
        return adapter.flushdbAsync()
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun getCacheEntry(iri: String, lang: String): CacheEntry? {
        val key = keyManager.toEntryKey(iri, lang)

        val hash = adapter
            .hmget(key, *CacheEntry.fields)
            .fold<Pair<String, String?>, MutableMap<String, String>>(mutableMapOf()) { e, (key, value) ->
                value?.let {
                    e[key] = it
                }
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
                { keyManager.toEntryKey(it.iri, lang) },
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
                adapter.hset(it.key, it.value)
                expiration?.let { cacheExpiration ->
                    adapter.expire(it.key, cacheExpiration)
                }
            }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun setString(vararg key: String, value: String, expiration: Long?) {
        val prefixed = keyManager.toKey(*key)
        adapter.set(prefixed, value)
        expiration?.let {
            val expiresAt = Instant.now().plusMillis(Duration.seconds(expiration).inWholeMilliseconds).toEpochMilli()
            strings[prefixed] = Pair(expiresAt, value)
            adapter.expire(prefixed, it)
        }
    }

    suspend fun keys(vararg pattern: String): Flow<List<String>> {
        val prefixed = keyManager.toKey(*pattern)

        return adapter
            .keys(prefixed)
            .map { keyManager.fromKey(it) }
    }

    suspend fun getString(vararg key: String): String? {
        val prefixed = keyManager.toKey(*key)
        strings[prefixed]?.let { (exp, value) ->
            if (Instant.now().toEpochMilli() < exp) {
                return value
            } else {
                strings.remove(prefixed)
            }
        }

        return adapter.get(prefixed)
    }

    suspend fun getHashValue(vararg key: String, hashKey: String): String? {
        val prefixed = keyManager.toKey(*key)

        return adapter.hget(prefixed, hashKey)
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, Storage> {
        override val key = AttributeKey<Storage>("Storage")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Storage {
            val config = Configuration()
                .apply {
                    keyManager = KeyManager(pipeline.cacheConfig.redis)
                }.apply(configure)

            val feature = Storage(
                config.adapter,
                config.keyManager,
                config.expiration,
            )
            pipeline.attributes.put(StorageKey, feature)

            val persistentFeature = Storage(
                config.persistentAdapter,
                config.keyManager,
                config.expiration,
            )
            pipeline.attributes.put(PersistedStorageKey, persistentFeature)

            return feature
        }
    }
}

private val StorageKey = AttributeKey<Storage>("StorageKey")
private val PersistedStorageKey = AttributeKey<Storage>("PersistedStorageKey")

internal val ApplicationCallPipeline.storage: Storage
    get() = attributes.getOrNull(StorageKey) ?: reportMissingRegistry()

internal val ApplicationCallPipeline.persistentStorage: Storage
    get() = attributes.getOrNull(PersistedStorageKey) ?: reportMissingRegistry()

private fun reportMissingRegistry(): Nothing {
    throw StorageNotYetConfiguredException()
}
class StorageNotYetConfiguredException :
    IllegalStateException("Logger is not yet ready: you are asking it to early before the Storage feature.")