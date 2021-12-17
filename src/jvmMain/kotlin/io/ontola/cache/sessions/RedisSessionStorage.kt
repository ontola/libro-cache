package io.ontola.cache.sessions

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.ontola.cache.plugins.RedisConfig
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.util.KeyManager

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
