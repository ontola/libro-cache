package io.ontola.cache.sessions

import io.ktor.server.sessions.SessionStorage
import io.ontola.cache.plugins.RedisConfig
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.util.KeyManager
import mu.KotlinLogging

private const val sessionPrefix = "session"

class RedisSessionStorage(
    private val redis: StorageAdapter<String, String>,
    config: RedisConfig,
) : SessionStorage {
    private val logger = KotlinLogging.logger {}
    private val keyManager = KeyManager(config)

    override suspend fun read(id: String): String {
        return redis.get(keyManager.toKey(sessionPrefix, id)) ?: throw NoSuchElementException()
    }

    override suspend fun write(id: String, value: String) {
        logger.trace { "write session $id" }
        redis.set(keyManager.toKey(sessionPrefix, id), value)
    }

    override suspend fun invalidate(id: String) {
        logger.trace { "invalidate session $id" }
        redis.del(keyManager.toKey(sessionPrefix, id))
    }
}
