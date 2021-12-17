package io.ontola.cache.util

import java.util.UUID

fun scopeBlankNodes(hex: String?): String? {
    if (hex == null) {
        return hex
    }
    val unique = UUID.randomUUID().toString()

    return hex.replace("\"_:", "\"_:$unique")
}
