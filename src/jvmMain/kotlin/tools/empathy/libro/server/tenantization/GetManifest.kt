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
 * Retrieves the [io.ontola.apex.webmanifest.Manifest] from the connected backend.
 */
internal suspend inline fun <reified K> ApplicationCall.getManifest(websiteBase: Url): K {
    val manifestUrl = websiteBase.appendPath("manifest.json")
    val manifestRequest = application.libroConfig.client.get(services.route(manifestUrl.fullPath)) {
        expectSuccess = false
        headers {
            append("Website-IRI", websiteBase.toString())

            proxySafeHeaders(request)
            copy(HttpHeaders.XForwardedFor, request)
            copy("X-Real-Ip", request)
        }
    }

    if (manifestRequest.status != HttpStatusCode.OK) {
        throw ResponseException(manifestRequest, manifestRequest.body())
    }

    return manifestRequest.body()
}
