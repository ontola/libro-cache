package io.ontola.cache.dataproxy

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.request.ApplicationRequest
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.isDownloadRequest
import io.ontola.cache.util.isHtmlAccept

class Configuration {
    var transforms = mutableMapOf<Regex, (req: ApplicationRequest) -> String>()
    var binaryPaths: List<String> = emptyList()
    var contentTypes: List<ContentType> = emptyList()
    var extensions: List<String> = emptyList()
    var excludedPaths: List<String> = emptyList()
    val unsafeList = listOf(
        CacheHttpHeaders.NewAuthorization.lowercase(),
        CacheHttpHeaders.NewRefreshToken.lowercase(),
        HttpHeaders.ContentLength.lowercase(),
        HttpHeaders.ContentType.lowercase(),
        HttpHeaders.TransferEncoding.lowercase(),
        HttpHeaders.Upgrade.lowercase(),
        *HttpHeaders.UnsafeHeadersList.toTypedArray()
    )
    val client = HttpClient(CIO) {
        followRedirects = false
        expectSuccess = false
    }

    fun isBinaryRequest(uri: Url, accept: String?): Boolean {
        return (binaryPaths.any { uri.encodedPath.contains(it) } && !accept.isHtmlAccept()) || uri.isDownloadRequest()
    }
}
