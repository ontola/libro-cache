package tools.empathy.libro.server.sessions

import io.ktor.server.sessions.SessionStorage
import tools.empathy.libro.server.configuration.RedisConfig
import tools.empathy.libro.server.plugins.StorageAdapter
import tools.empathy.libro.server.util.KeyManager

private const val sessionPrefix = "session"

class RedisSessionStorage(
    private val redis: StorageAdapter<String, String>,
    config: RedisConfig,
) : SessionStorage {
    private val keyManager = KeyManager(config)

    override suspend fun read(id: String): String {
        return redis.get(keyManager.toKey(sessionPrefix, id)) ?: throw NoSuchElementException()
    }

    override suspend fun write(id: String, value: String) {
        redis.set(keyManager.toKey(sessionPrefix, id), value)
    }

    override suspend fun invalidate(id: String) {
        redis.del(keyManager.toKey(sessionPrefix, id))
    }
}
