package io.ontola.cache.util

import io.ktor.http.*

fun Url.origin(): String {
    return "${this.protocol.name}://${this.authority}"
}
