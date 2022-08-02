package tools.empathy.libro.server.tenantization

import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.path
import tools.empathy.libro.server.TenantNotFoundException
import tools.empathy.libro.server.WrongWebsiteIRIException
import tools.empathy.libro.server.plugins.persistentStorage
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.libro.server.util.origin
import tools.empathy.url.asHrefString
import tools.empathy.url.origin

private fun Boolean.toInt(): Int = if (this) 1 else 0

/**
 * Returns the websiteIRI when found in storage.
 */
internal suspend fun ApplicationRequest.lookupWebsiteBase(websiteIRI: String): Boolean {
    if (call.application.persistentStorage.hexists(CachedLookupKeys.Manifest.name, hashKey = websiteIRI)) {
        return true
    }

    return false
}

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

internal suspend fun ApplicationRequest.websiteBaseFromUrl(): String? {
    val requestPath = path()
        .removeSuffix("link-lib/bulk")
        .split("/")
        .take(2)
        .joinToString("/")
        .removeSuffix("/")
    val authoritativeOrigin = origin()

    val checkUrls = setOf(
        Url("$authoritativeOrigin$requestPath".removeSuffix("/")).asHrefString,
        Url(authoritativeOrigin).asHrefString
    )

    return checkUrls.find { lookupWebsiteBase(it) }
}

@Throws(TenantNotFoundException::class)
internal suspend fun ApplicationCall.getWebsiteBase(): String = request.websiteBaseFromHeader()
    ?: request.websiteBaseFromUrl()
    ?: throw TenantNotFoundException()
