package tools.empathy.libro.server.sessions

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import tools.empathy.libro.server.plugins.RedisConfig
import tools.empathy.libro.server.plugins.StorageAdapter
import tools.empathy.libro.server.util.KeyManager

private const val sessionPrefix = "session"

class RedisSessionStorage(
    private val redis: StorageAdapter<String, String>,
    config: RedisConfig,
) : SimplifiedSessionStorage() {
    private val keyManager = KeyManager(config)

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override suspend fun read(id: String): ByteArray? {
        return redis.get(keyManager.toKey(sessionPrefix, id))?.toByteArray(Charsets.UTF_8)
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override suspend fun write(id: String, data: ByteArray?) {
        data?.let {
            redis.set(keyManager.toKey(sessionPrefix, id), it.decodeToString())
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override suspend fun invalidate(id: String) {
        redis.del(keyManager.toKey(sessionPrefix, id))
    }
}
