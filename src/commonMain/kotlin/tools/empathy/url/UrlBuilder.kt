package tools.empathy.url

import io.ktor.http.URLBuilder

fun URLBuilder.appendPath(vararg segments: String) {
    pathSegments = pathSegments.filter(String::isNotBlank) + segments.filter(String::isNotBlank)

    if (pathSegments.isEmpty()) {
        pathSegments = listOf("")
    }
}
fun URLBuilder.appendPath(segments: List<String>) {
    appendPath(*segments.toTypedArray())
}
