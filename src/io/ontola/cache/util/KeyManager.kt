package io.ontola.cache.util

import io.ontola.cache.features.CacheConfig
import java.net.URLEncoder

class KeyManager(
    private val config: CacheConfig,
) {
    private val separator = config.redis.separator
    private val encodedSeparator = URLEncoder.encode(config.redis.separator, "utf-8")

    private val prefixes = listOfNotNull(
        config.redis.rootPrefix,
        config.redis.cachePrefix,
        config.redis.cacheEntryPrefix,
    ).toTypedArray()
    private val iriIndex = prefixes.size
    private val langIndex = iriIndex + 1

    fun toKey(iri: String, lang: String = config.defaultLanguage): String {
        return listOfNotNull(*prefixes, encode(iri), lang).joinToString(separator)
    }

    fun fromKey(key: String): Pair<String, String> {
        val split = key.split(separator)

        return Pair(decode(split[iriIndex]), split[langIndex])
    }

    private fun decode(iri: String): String = iri.replace(encodedSeparator, separator)

    private fun encode(iri: String): String = iri.replace(separator, encodedSeparator)
}
