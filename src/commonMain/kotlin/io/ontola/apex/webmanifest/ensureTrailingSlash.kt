package io.ontola.apex.webmanifest

fun ensureTrailingSlash(value: String) = if (value.endsWith('/')) value else "$value/"
