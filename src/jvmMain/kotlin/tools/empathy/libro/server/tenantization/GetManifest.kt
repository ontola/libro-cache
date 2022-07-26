package tools.empathy.libro.server.tenantization

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.server.application.ApplicationCall
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.plugins.services
import tools.empathy.libro.server.util.copy
import tools.empathy.libro.server.util.proxySafeHeaders
import tools.empathy.url.appendPath

/**
 * Retrieves the [tools.empathy.libro.webmanifest.Manifest] from the connected backend.
 */
internal suspend inline fun <reified K> ApplicationCall.getManifest(websiteBase: Url, isCors: Boolean): K {
    val manifestUrl = websiteBase.appendPath("manifest.json")
    val routedManifestUrl = if (isCors) {
        manifestUrl
    } else {
        services.route(manifestUrl.fullPath)
    }

    val manifestRequest = application.libroConfig.client.get(routedManifestUrl) {
        expectSuccess = false
        headers {
            append("Website-IRI", websiteBase.toString())

            proxySafeHeaders(request)
            if (isCors) {
                set(HttpHeaders.XForwardedHost, websiteBase.host)
            }
            copy(HttpHeaders.XForwardedFor, request)
            copy("X-Real-Ip", request)
        }
    }

    if (manifestRequest.status != HttpStatusCode.OK) {
        throw ResponseException(manifestRequest, manifestRequest.body())
    }

    return manifestRequest.body()
}
