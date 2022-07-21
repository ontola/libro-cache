package tools.empathy.libro.webmanifest

internal fun ensureTrailingSlash(value: String) = if (value.endsWith('/')) value else "$value/"
