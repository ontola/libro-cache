package tools.empathy.libro.server.configuration

import io.lettuce.core.RedisURI

data class RedisConfig(
    /**
     * The redis uri where the cache writes resources to.
     */
    val uri: RedisURI,

    /**
     * Root prefix to prepend to all keys.
     */
    val rootPrefix: String? = null,

    /**
     * Cache-specific prefix to append to the [rootPrefix] but before all other keys.
     */
    val cachePrefix: String? = "cache",

    /**
     * Key part for cache entries.
     */
    val cacheEntryPrefix: String = "entry",

    /**
     * Separator to use inbetween key parts
     * https://redis.io/topics/data-types-intro#redis-keys
     */
    val separator: String = ":",
    /**
     * The channel where invalidation messages will be broadcast.
     */
    val invalidationChannel: String,
    /**
     * The group used when reading from the [invalidationChannel].
     */
    val invalidationGroup: String,
)
