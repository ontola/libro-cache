package io.ontola.util

import io.ktor.http.URLBuilder

fun URLBuilder.appendPath(vararg segments: String) {
    pathSegments = pathSegments.filter(String::isNotBlank) + segments.filter(String::isNotBlank)
}
fun URLBuilder.appendPath(segments: List<String>) {
    appendPath(*segments.toTypedArray())
}
