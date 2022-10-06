package tools.empathy.libro.server.tenantization

import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.path
import tools.empathy.libro.server.TenantNotFoundException
import tools.empathy.libro.server.WrongWebsiteIRIException
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.libro.server.util.origin
import tools.empathy.url.asHrefString
import tools.empathy.url.origin

/**
 * Returns the WebsiteIRI header when present
 */
@Throws(WrongWebsiteIRIException::class)
internal fun ApplicationRequest.websiteBaseFromHeader(): String? {
    val authoritativeOrigin = origin()

    return headers[LibroHttpHeaders.WebsiteIri]?.let {
        if (Url(it).origin() != authoritativeOrigin) {
            throw WrongWebsiteIRIException()
        }
        it
    }
}

internal fun ApplicationRequest.checkUrls(): Set<String> {
    val requestPath = path()
        .removeSuffix("link-lib/bulk")
        .split("/")
        .take(2)
        .joinToString("/")
        .removeSuffix("/")
    val authoritativeOrigin = origin()

    return setOf(
        Url("$authoritativeOrigin$requestPath".removeSuffix("/")).asHrefString,
        Url(authoritativeOrigin).asHrefString
    )
}

internal suspend fun ApplicationRequest.websiteBaseFromUrl(): String? =
    checkUrls().find { manifestExists(it) }

@Throws(TenantNotFoundException::class)
internal suspend fun ApplicationCall.getWebsiteBaseOrNull(): String? =
    request.websiteBaseFromHeader() ?: request.websiteBaseFromUrl()

@Throws(TenantNotFoundException::class)
internal suspend fun ApplicationCall.getWebsiteBase(): String =
    getWebsiteBaseOrNull() ?: throw TenantNotFoundException("not registered")
