package io.ontola.cache.util

fun String?.isHtmlAccept(): Boolean = this != null && (
    this == "*/*" ||
        contains("text/html") ||
        contains("application/xhtml+xml") ||
        contains("application/xml")
    )
