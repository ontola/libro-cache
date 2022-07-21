package tools.empathy.libro.server.util

import io.ktor.server.request.ApplicationRequest

/**
 * Get the origin for the current request.
 * Uses security headers to verify other information when present.
 */
internal fun ApplicationRequest.origin(): String {
    val authority = listOf("X-Forwarded-Host", "origin", "host", "authority")
        .find { header -> headers[header] != null }
        ?.let { header -> headers[header]!! }
        ?: throw Exception("No header usable for authority present")

    if (authority == "localhost" && call.application.developmentMode) {
        return "http://$authority"
    }

    val proto = headers["X-Forwarded-Proto"]?.split(',')?.firstOrNull()
        ?: headers["origin"]?.split(":")?.firstOrNull()
        ?: throw Exception("No Forwarded host nor authority scheme")

    return if (authority.contains(':')) {
        authority
    } else {
        "$proto://$authority"
    }
}
