package io.ontola.cache.tenantization

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
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.services
import io.ontola.cache.util.copy
import io.ontola.cache.util.proxySafeHeaders

internal suspend inline fun <reified K> PipelineContext<*, ApplicationCall>.getManifest(websiteBase: Url): K {
    val manifestRequest = application.cacheConfig.client.get(call.services.route("${websiteBase.fullPath}/manifest.json")) {
        expectSuccess = false
        headers {
            append("Website-IRI", websiteBase.toString())

            proxySafeHeaders(context.request)
            copy(HttpHeaders.XForwardedFor, context.request)
            copy("X-Real-Ip", context.request)
        }
    }

    if (manifestRequest.status != HttpStatusCode.OK) {
        throw ResponseException(manifestRequest, manifestRequest.body())
    }

    return manifestRequest.body()
}
