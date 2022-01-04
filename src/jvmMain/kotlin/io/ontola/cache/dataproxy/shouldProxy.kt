package io.ontola.cache.dataproxy

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.parseHeaderValue
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.accept
import io.ktor.server.request.document
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri

internal fun Configuration.shouldProxy(call: ApplicationCall): Boolean {
    val accept = parseHeaderValue(call.request.accept() ?: "*/*")
        .map { v -> ContentType.parse(v.value).withoutParameters() }
        .toSet()
    val uri = Url(call.request.uri)
    val path = call.request.path()

    val isManifestRequest = call.request.document() == "manifest.json"
    val isPathExcluded = excludedPaths.contains(path)
    val isNotExcluded = !isManifestRequest && !isPathExcluded

    val ext = if (path.contains(".")) path.split(".").lastOrNull() else null
    val isDataReqByExtension = ext?.let { extensions.contains(it) } ?: false
    val isDataReqByAccept = contentTypes.intersect(accept).isNotEmpty()
    val isBinaryRequest = isBinaryRequest(uri)
    val isBlindPost = blindPostPaths.any { uri.encodedPath.contains(it) } && call.request.httpMethod === HttpMethod.Post
    val isProxyableComponent = isDataReqByExtension || isDataReqByAccept || isBinaryRequest || isBlindPost

    return isNotExcluded && isProxyableComponent
}
