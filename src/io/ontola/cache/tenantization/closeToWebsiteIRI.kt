package io.ontola.cache.tenantization

import io.ktor.http.Headers
import io.ktor.http.Url
import io.ontola.cache.util.origin
import mu.KLogger

private fun Boolean.toInt(): Int = if (this) 1 else 0

internal fun closeToWebsiteIRI(requestPath: String, headers: Headers, logger: KLogger): String {
    val path = requestPath.removeSuffix("link-lib/bulk")
    val authority = listOf("X-Forwarded-Host", "origin", "host", "authority")
        .find { header -> headers[header] != null }
        ?.let { header -> headers[header]!! }
        ?: throw Exception("No header usable for authority present")

//        if (authority.contains(':')) {
//            return "$authority$path"
//        }

    val proto = headers["X-Forwarded-Proto"]?.split(',')?.firstOrNull()
        ?: headers["origin"]?.split(":")?.firstOrNull()
        ?: throw Exception("No Forwarded host nor authority scheme")

    val authoritativeOrigin = if (authority.contains(':')) {
        authority
    } else {
        "$proto://$authority"
    }

    return headers["Website-IRI"]
        ?.let { websiteIRI ->
            if (Url(websiteIRI).origin() != authoritativeOrigin) {
                logger.warn("Website-Iri does not correspond with authority headers (website-iri: '$websiteIRI', authority: '$authoritativeOrigin')")
            }
            websiteIRI
        } ?: "$authoritativeOrigin$path".let { it.dropLast(it.endsWith('/').toInt()) }
}
