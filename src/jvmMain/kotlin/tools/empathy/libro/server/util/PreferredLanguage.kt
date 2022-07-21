package tools.empathy.libro.server.util

fun String.preferredLanguage(): String? = split(",").firstOrNull()?.split("-")?.firstOrNull()
