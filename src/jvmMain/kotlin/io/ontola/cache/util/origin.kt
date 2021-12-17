package io.ontola.cache.util

import io.ktor.server.request.ApplicationRequest

internal fun ApplicationRequest.origin(): String {
    val authority = listOf("X-Forwarded-Host", "origin", "host", "authority")
        .find { header -> headers[header] != null }
        ?.let { header -> headers[header]!! }
        ?: throw Exception("No header usable for authority present")

    val proto = headers["X-Forwarded-Proto"]?.split(',')?.firstOrNull()
        ?: headers["origin"]?.split(":")?.firstOrNull()
        ?: throw Exception("No Forwarded host nor authority scheme")

    return if (authority.contains(':')) {
        authority
    } else {
        "$proto://$authority"
    }
}
