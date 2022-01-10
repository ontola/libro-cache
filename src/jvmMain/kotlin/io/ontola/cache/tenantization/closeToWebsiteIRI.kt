package io.ontola.cache.tenantization

import io.ktor.http.Url
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.path
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.origin
import mu.KLogger

private fun Boolean.toInt(): Int = if (this) 1 else 0

internal fun ApplicationRequest.closeToWebsiteIRI(logger: KLogger): String {
    val authoritativeOrigin = origin()
    val requestPath = path()
        .removeSuffix("link-lib/bulk")
        .split("/")
        .take(2)
        .joinToString("/")

    return headers[CacheHttpHeaders.WebsiteIri]
        ?.let { websiteIRI ->
            if (Url(websiteIRI).origin() != authoritativeOrigin) {
                logger.warn("Website-Iri does not correspond with authority headers (website-iri: '$websiteIRI', authority: '$authoritativeOrigin')")
            }
            websiteIRI
        } ?: "$authoritativeOrigin$requestPath".let { it.dropLast(it.endsWith('/').toInt()) }
}
