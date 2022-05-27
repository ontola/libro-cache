/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ontola.cache.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.escapeIfNeeded
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.plugins.origin
import io.ktor.server.request.host
import io.ktor.server.response.header

/**
 *  A configuration for the [HSTS] plugin.
 */
class HSTSConfig {
    /**
     * Specifies the `preload` HSTS directive, which allows you to include your domain name
     * in the HSTS preload list.
     */
    var preload: Boolean = false

    /**
     * Specifies the `includeSubDomains` directive, which applies this policy to any subdomains as well.
     */
    var includeSubDomains: Boolean = true

    /**
     * Specifies how long (in seconds) the client should keep the host in a list of known HSTS hosts:
     */
    var maxAgeInSeconds: Long = DEFAULT_HSTS_MAX_AGE
        set(newMaxAge) {
            check(newMaxAge >= 0L) { "maxAgeInSeconds shouldn't be negative: $newMaxAge" }
            field = newMaxAge
        }

    /**
     * Allows you to add custom directives supported by a specific user agent.
     */
    val customDirectives: MutableMap<String, String?> = HashMap()

    /**
     * @see [withHost]
     */
    internal val hostSpecific: MutableMap<String, HSTSConfig> = HashMap()

    /**
     * Set specific configuration for a [host].
     */
    fun withHost(host: String, configure: HSTSConfig.() -> Unit) {
        this.hostSpecific[host] = HSTSConfig().apply(configure)
    }
}

internal const val DEFAULT_HSTS_MAX_AGE: Long = 365L * 24 * 3600 // 365 days

/**
 * A plugin that appends the `Strict-Transport-Security` HTTP header to every response.
 *
 * The [HSTS] configuration below specifies how long the client
 * should keep the host in a list of known HSTS hosts:
 * ```kotlin
 * install(HSTS) {
 *     maxAgeInSeconds = 10
 * }
 * ```
 * You can learn more from [HSTS](https://ktor.io/docs/hsts.html).
 */
val HSTS = createApplicationPlugin("HSTS", ::HSTSConfig) {
    fun constructHeaderValue(config: HSTSConfig) = buildString {
        append("max-age=")
        append(config.maxAgeInSeconds)

        if (config.includeSubDomains) {
            append("; includeSubDomains")
        }
        if (config.preload) {
            append("; preload")
        }

        if (config.customDirectives.isNotEmpty()) {
            config.customDirectives.entries.joinTo(this, separator = "; ", prefix = "; ") {
                if (it.value != null) {
                    "${it.key.escapeIfNeeded()}=${it.value?.escapeIfNeeded()}"
                } else {
                    it.key.escapeIfNeeded()
                }
            }
        }
    }

    /**
     * A constructed default `Strict-Transport-Security` header value.
     */

    /**
     * A constructed default `Strict-Transport-Security` header value.
     */
    /**
     * A constructed default `Strict-Transport-Security` header value.
     */
    /**
     * A constructed default `Strict-Transport-Security` header value.
     */
    val headerValue: String = constructHeaderValue(pluginConfig)

    /**
     * Constructed `Strict-Transport-Security` header value for given hosts
     */

    /**
     * Constructed `Strict-Transport-Security` header value for given hosts
     */
    /**
     * Constructed `Strict-Transport-Security` header value for given hosts
     */
    /**
     * Constructed `Strict-Transport-Security` header value for given hosts
     */
    val hostHeaderValues: Map<String, String> = pluginConfig.hostSpecific.mapValues { constructHeaderValue(it.value) }

    onCall { call ->
        if (call.request.origin.run { scheme == "https" && port == 443 }) {
            call.response.header(
                HttpHeaders.StrictTransportSecurity,
                hostHeaderValues[call.request.host()] ?: headerValue
            )
        }
    }
}
