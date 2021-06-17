package io.ontola.cache.util

import io.ontola.cache.features.CacheConfig

class KeyManager(
    private val config: CacheConfig,
) {
    private val prefixes = listOfNotNull(
        config.redis.rootPrefix,
        config.redis.cachePrefix,
        config.redis.cacheEntryPrefix,
    ).toTypedArray()

    fun toKey(iri: String, lang: String = config.defaultLanguage): String {
        return listOfNotNull(*prefixes, iri, lang).joinToString(config.redis.separator)
    }

    fun fromKey(key: String): String {
        return key.split(config.redis.separator)[prefixes.size + 1]
    }
}
