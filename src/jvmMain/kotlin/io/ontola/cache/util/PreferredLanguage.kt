package io.ontola.cache.util

fun String.preferredLanguage(): String? = split(",").firstOrNull()?.split("-")?.firstOrNull()
