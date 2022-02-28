package io.ontola.cache.util

class KeyMap(private vararg val parts: String) {
    fun getPart(key: List<String>, part: String): String = key[parts.indexOf(part)]
}
