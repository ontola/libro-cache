package io.ontola.cache.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationPlugin
import io.ktor.util.AttributeKey
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.FlushMode
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.ontola.cache.bulk.CacheControl
import io.ontola.cache.bulk.CacheEntry
import io.ontola.cache.util.KeyManager
import io.ontola.empathy.web.DataSlice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

interface StorageAdapter<K : Any, V : Any> {
    suspend fun del(key: K): Long?

    suspend fun expire(key: K, seconds: Long): Boolean?

    suspend fun get(key: K): V?

    suspend fun hdel(key: K, vararg fields: V): Long?

    suspend fun hget(key: String, field: String): String?

    fun hgetall(key: K): Flow<Pair<K, V>>

    fun hmget(key: K, vararg fields: K): Flow<Pair<K, V?>>

    suspend fun hset(key: K, map: Map<K, V>): Long?

    suspend fun flushdbAsync(): String?

    suspend fun keys(pattern: K): Flow<String>

    suspend fun lgetall(key: String): List<String>

    suspend fun lsetall(key: String, values: List<String>): Long?

    suspend fun incr(key: String): Long?

    suspend fun lrange(key: String, start: Long, stop: Long): List<String>

    suspend fun sadd(key: K, vararg members: V): Long?

    suspend fun set(key: K, value: V): String?

    suspend fun smembers(key: K): Flow<V>
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisAdapter(val client: RedisCoroutinesCommands<String, String>) : StorageAdapter<String, String> {
    override suspend fun del(key: String): Long? {
        return client.del(key)
    }

    override suspend fun hdel(key: String, vararg fields: String): Long? {
        return client.hdel(key, *fields)
    }

    override suspend fun expire(key: String, seconds: Long): Boolean? {
        return client.expire(key, seconds)
    }

    override suspend fun hget(key: String, field: String): String? {
        return client.hget(key, field)
    }

    override fun hgetall(key: String): Flow<Pair<String, String>> {
        return client.hgetall(key).map { Pair(it.key, it.getValueOrElse(null)) }
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

    override suspend fun lgetall(key: String): List<String> {
        return client.lrange(key, 0, -1)
    }

    override suspend fun lsetall(key: String, values: List<String>): Long? {
        client.del(key)
        return client.lpush(key, *values.toTypedArray())
    }

    override suspend fun lrange(key: String, start: Long, stop: Long): List<String> {
        return client.lrange(key, start, stop)
    }

    override suspend fun incr(key: String): Long? {
        return client.incr(key)
    }

    override suspend fun sadd(key: String, vararg members: String): Long? {
        return client.sadd(key, *members)
    }

    override suspend fun set(key: String, value: String): String? {
        return client.set(key, value)
    }

    override suspend fun smembers(key: String): Flow<String> {
        return client.smembers(key)
    }
}

private val logger = KotlinLogging.logger {}

class Storage(
    private val adapter: StorageAdapter<String, String>,
    private val keyManager: KeyManager,
    private val expiration: Long?,
) {

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
            contents = hash["contents"]?.let { Json.decodeFromString<DataSlice>(it) },
        )
    }

    suspend fun setAdd(vararg key: String, member: String): Long? {
        val prefixed = keyManager.toKey(*key)

        return adapter.sadd(prefixed, member)
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
                        "contents" to Json.encodeToString(it.contents.orEmpty()),
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
            adapter.expire(prefixed, it)
        }
    }

    suspend fun deleteKey(vararg key: String): Long? {
        val prefixed = keyManager.toKey(*key)

        return adapter.del(prefixed)
    }

    suspend fun keys(vararg pattern: String): Flow<List<String>> {
        val prefixed = keyManager.toKey(*pattern)

        return adapter
            .keys(prefixed)
            .map { keyManager.fromKey(it) }
    }

    suspend fun getSet(vararg key: String): Set<String>? {
        val prefixed = keyManager.toKey(*key)

        return adapter.smembers(prefixed).toSet()
    }

    suspend fun getString(vararg key: String): String? {
        val prefixed = keyManager.toKey(*key)

        return adapter.get(prefixed)
    }

    suspend fun getHash(vararg key: String): Map<String, String> {
        val prefixed = keyManager.toKey(*key)

        return adapter
            .hgetall(prefixed)
            .fold(mutableMapOf()) { map, (k, v) ->
                map[k] = v
                map
            }
    }

    suspend fun getHashValue(vararg key: String, hashKey: String): String? {
        val prefixed = keyManager.toKey(*key)

        return adapter.hget(prefixed, hashKey)
    }

    suspend fun deleteHashValue(vararg key: String, hashKey: String): Boolean {
        val prefixed = keyManager.toKey(*key)

        val removed = adapter.hdel(prefixed, hashKey) ?: 0

        return removed > 0
    }

    suspend fun setHashValues(vararg key: String, entries: Map<String, String>): Long? {
        val prefixed = keyManager.toKey(*key)

        return adapter.hset(prefixed, entries)
    }

    suspend fun getAllListValues(vararg key: String): List<String> {
        val prefixed = keyManager.toKey(*key)

        return adapter.lgetall(prefixed)
    }

    suspend fun setAllListValues(vararg key: String, values: List<String>): Long? {
        val prefixed = keyManager.toKey(*key)

        return adapter.lsetall(prefixed, values)
    }

    suspend fun increment(vararg key: String): Long? {
        val prefixed = keyManager.toKey(*key)

        return adapter.incr(prefixed)
    }

    companion object Plugin : ApplicationPlugin<ApplicationCallPipeline, Configuration, Storage> {
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
