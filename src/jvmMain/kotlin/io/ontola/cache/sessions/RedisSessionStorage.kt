package io.ontola.cache.sessions

import io.ktor.server.sessions.SessionStorage
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.ontola.cache.plugins.RedisConfig
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.util.KeyManager

private const val sessionPrefix = "session"

class RedisSessionStorage(
    private val redis: StorageAdapter<String, String>,
    config: RedisConfig,
) : SessionStorage {
    private val keyManager = KeyManager(config)

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override suspend fun read(id: String): String = redis.get(keyManager.toKey(sessionPrefix, id))
        ?: throw NoSuchElementException()

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override suspend fun write(id: String, value: String) {
        redis.set(keyManager.toKey(sessionPrefix, id), value)
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override suspend fun invalidate(id: String) {
        redis.del(keyManager.toKey(sessionPrefix, id))
    }
}
