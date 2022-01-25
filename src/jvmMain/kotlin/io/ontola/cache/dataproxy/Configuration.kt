package io.ontola.cache.dataproxy

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.request.ApplicationRequest
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.configureClientLogging
import io.ontola.util.disableCertValidation

enum class ProxyClient {
    VerbatimBackend,
    RedirectingBackend,
    Binary,
}

data class ProxyRule(
    /**
     * Determines if the rule is active
     */
    val match: Regex,
    /**
     * Set to true to make the rule exclude matches from proxying.
     */
    val exclude: Boolean = false,
    /**
     * Whether to send credentials in the proxied request.
     */
    val includeCredentials: Boolean = true,
    /**
     * The client to use if this rule matches.
     */
    val client: ProxyClient = ProxyClient.VerbatimBackend,
)

class Configuration {
    /**
     * Allows request paths to be transformed before being sent to the proxy destination.
     */
    var transforms = mutableMapOf<Regex, (req: ApplicationRequest) -> String>()

    /**
     * These methods will be proxied.
     */
    var methods: List<HttpMethod> = emptyList()

    /**
     * These content types will be proxied if received in the 'Accept' header.
     */
    var contentTypes: List<ContentType> = emptyList()

    var rules: List<ProxyRule> = emptyList()

    /**
     * These extensions will be proxied.
     */
    var extensions: List<String> = emptyList()

    var skipCertificateValidation: Boolean = false

    val unsafeList = listOf(
        CacheHttpHeaders.NewAuthorization.lowercase(),
        CacheHttpHeaders.NewRefreshToken.lowercase(),
        HttpHeaders.ContentLength.lowercase(),
        HttpHeaders.ContentType.lowercase(),
        HttpHeaders.SetCookie.lowercase(),
        HttpHeaders.TransferEncoding.lowercase(),
        HttpHeaders.Upgrade.lowercase(),
        *HttpHeaders.UnsafeHeadersList.toTypedArray()
    )
    val verbatimClient = HttpClient(CIO) {
        followRedirects = false
        expectSuccess = false
        install(Logging) {
            configureClientLogging()
        }
        if (skipCertificateValidation) disableCertValidation()
    }
    val redirectingClient = HttpClient(CIO) {
        followRedirects = true
        expectSuccess = false
        install(Logging) {
            configureClientLogging()
        }
        if (skipCertificateValidation) disableCertValidation()
    }
    lateinit var binaryClient: HttpClient

    var defaultRule = ProxyRule(
        match = Regex(""),
        exclude = false,
        includeCredentials = true,
        client = ProxyClient.RedirectingBackend,
    )

    fun match(path: String): ProxyRule? {
        return rules.find { !it.exclude && it.match.containsMatchIn(path) }
    }

    fun matchOrDefault(path: String): ProxyRule {
        return match(path) ?: defaultRule
    }
}
