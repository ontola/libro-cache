package io.ontola.cache.tenantization

import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.client.call.receive
import io.ktor.client.features.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.services
import io.ontola.cache.util.copy
import io.ontola.cache.util.proxySafeHeaders

internal suspend inline fun <reified K> PipelineContext<*, ApplicationCall>.getManifest(websiteBase: Url): K {
    val manifestRequest = application.cacheConfig.client.get<HttpResponse>(call.services.route("${websiteBase.fullPath}/manifest.json")) {
        headers {
            header("Website-IRI", websiteBase)

            proxySafeHeaders(context.request)
            copy(HttpHeaders.XForwardedFor, context.request)
            copy("X-Real-Ip", context.request)
        }
    }

    if (manifestRequest.status != HttpStatusCode.OK) {
        throw ResponseException(manifestRequest, manifestRequest.receive())
    }

    return manifestRequest.receive()
}
