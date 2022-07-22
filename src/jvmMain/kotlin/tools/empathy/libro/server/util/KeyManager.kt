package tools.empathy.libro.server.util

import tools.empathy.libro.server.configuration.RedisConfig
import java.net.URLEncoder

class KeyManager(
    config: RedisConfig,
) {
    private val separator = config.separator
    private val encodedSeparator = URLEncoder.encode(config.separator, "utf-8")
    private val cacheEntryPrefix = config.cacheEntryPrefix

    private val cachePrefixes = listOfNotNull(
        config.rootPrefix,
        config.cachePrefix,
    ).toTypedArray()
    private val cachePrefix = cachePrefixes.joinToString(separator) + ":"

    private val iriIndex = cachePrefixes.size + 1
    private val langIndex = iriIndex + 1

    fun toEntryKey(iri: String, lang: String): String = toKey(cacheEntryPrefix, encode(iri), lang)

    fun fromEntryKey(key: String): Pair<String, String> {
        val split = key.split(separator)

        return Pair(decode(split[iriIndex]), split[langIndex])
    }

    fun toKey(vararg components: String): String = listOfNotNull(*cachePrefixes, *components.map { encode(it) }.toTypedArray())
        .joinToString(separator)

    fun fromKey(key: String): List<String> = key.removePrefix(cachePrefix).split(separator).map { decode(it) }

    private fun decode(iri: String): String = iri.replace(encodedSeparator, separator)

    private fun encode(iri: String): String = iri.replace(separator, encodedSeparator)
}
