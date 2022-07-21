package tools.empathy.libro.server.util

fun String?.isHtmlAccept(): Boolean = this != null && (
    this == "*/*" ||
        contains("text/html") ||
        contains("application/xhtml+xml") ||
        contains("application/xml")
    )
