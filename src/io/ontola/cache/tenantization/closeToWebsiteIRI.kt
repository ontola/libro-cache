package io.ontola.cache.tenantization

import io.ktor.http.Url
import io.ktor.request.ApplicationRequest
import io.ktor.request.path
import io.ontola.cache.util.origin
import mu.KLogger

private fun Boolean.toInt(): Int = if (this) 1 else 0

internal fun ApplicationRequest.closeToWebsiteIRI(logger: KLogger): String {
    val authoritativeOrigin = origin()
    val requestPath = path().removeSuffix("link-lib/bulk")

    return headers["Website-IRI"]
        ?.let { websiteIRI ->
            if (Url(websiteIRI).origin() != authoritativeOrigin) {
                logger.warn("Website-Iri does not correspond with authority headers (website-iri: '$websiteIRI', authority: '$authoritativeOrigin')")
            }
            websiteIRI
        } ?: "$authoritativeOrigin$requestPath".let { it.dropLast(it.endsWith('/').toInt()) }
}
