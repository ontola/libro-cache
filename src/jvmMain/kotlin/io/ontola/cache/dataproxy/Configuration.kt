package io.ontola.cache.dataproxy

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.server.request.ApplicationRequest
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.isDownloadRequest

class Configuration {
    /**
     * Allows request paths to be transformed before being sent to the proxy destination.
     */
    var transforms = mutableMapOf<Regex, (req: ApplicationRequest) -> String>()

    /**
     * Included paths and sub-paths will be proxied if not excluded otherwise.
     */
    var binaryPaths: List<String> = emptyList()

    /**
     * These methods will be proxied.
     */
    var methods: List<HttpMethod> = emptyList()

    /**
     * These content types will be proxied if received in the 'Accept' header.
     */
    var contentTypes: List<ContentType> = emptyList()

    /**
     * These extensions will be proxied.
     */
    var extensions: List<String> = emptyList()

    /**
     * These paths will not be proxied.
     * Paths are matched exactly. Exclusion criteria take precedence over inclusion criteria.
     */
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
    lateinit var binaryClient: HttpClient

    fun isBinaryRequest(uri: Url): Boolean {
        return binaryPaths.any { uri.encodedPath.contains(it) } || uri.isDownloadRequest()
    }
}
