package tools.empathy.libro.server.tenantization

import io.ktor.http.Url
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.path
import mu.KLogger
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.libro.server.util.origin
import tools.empathy.url.origin

private fun Boolean.toInt(): Int = if (this) 1 else 0

/**
 * Returns the origin with the first path segment when present.
 */
internal fun ApplicationRequest.closeToWebsiteIRI(logger: KLogger): String {
    val authoritativeOrigin = origin()
    val requestPath = path()
        .removeSuffix("link-lib/bulk")
        .split("/")
        .take(2)
        .joinToString("/")

    return headers[LibroHttpHeaders.WebsiteIri]
        ?.let { websiteIRI ->
            if (Url(websiteIRI).origin() != authoritativeOrigin) {
                logger.warn("Website-Iri does not correspond with authority headers (website-iri: '$websiteIRI', authority: '$authoritativeOrigin')")
            }
            websiteIRI
        } ?: "$authoritativeOrigin$requestPath".let { it.dropLast(it.endsWith('/').toInt()) }
}
