package io.ontola.cache.sessions

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.ontola.cache.plugins.StorageAdapter

class RedisSessionStorage(val redis: StorageAdapter<String, String>) : SimplifiedSessionStorage() {
    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override suspend fun read(id: String): ByteArray? {
        return redis.get(id)?.toByteArray(Charsets.UTF_8)
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override suspend fun write(id: String, data: ByteArray?) {
        data?.let {
            redis.set(id, it.decodeToString())
        }
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    override suspend fun invalidate(id: String) {
        redis.del(id)
    }
}
