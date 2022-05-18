package io.ontola.cache.tenantization

import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.server.application.ApplicationCall
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.services
import io.ontola.cache.util.copy
import io.ontola.cache.util.proxySafeHeaders
import io.ontola.util.appendPath

internal suspend inline fun ApplicationCall.getManifest(websiteBase: Url): String {
    val manifestUrl = websiteBase.appendPath("manifest.json")
    val manifestRequest = application.cacheConfig.client.get(services.route(manifestUrl.fullPath)) {
        expectSuccess = false
        headers {
            append("Website-IRI", websiteBase.toString())

            proxySafeHeaders(request)
            copy(HttpHeaders.XForwardedFor, request)
            copy("X-Real-Ip", request)
        }
    }

    if (manifestRequest.status != HttpStatusCode.OK) {
        throw ResponseException(manifestRequest, manifestRequest.bodyAsText())
    }

    return manifestRequest.bodyAsText()
}
