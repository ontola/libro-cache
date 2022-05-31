@file:UseSerializers(UrlSerializer::class)
package io.ontola.cache.plugins

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import io.ontola.cache.util.UrlSerializer
import io.ontola.studio.StudioDeploymentKey
import kotlinx.serialization.UseSerializers
import mu.KotlinLogging

private val BlacklistedKey = AttributeKey<Boolean>("BlacklistedKey")

internal val ApplicationCall.blacklisted: Boolean
    get() = attributes.getOrNull(BlacklistedKey) ?: reportMissingBlacklist()

internal fun ApplicationCall.blacklist() = attributes.put(BlacklistedKey, true)

private fun ApplicationCall.reportMissingBlacklist(): Nothing {
    application.plugin(Blacklist) // ensure the feature is installed
    throw BlacklistNotYetConfiguredException()
}

class BlacklistNotYetConfiguredException : IllegalStateException("Blacklist not ready, ensure plugin is already loaded")

class BlacklistConfiguration {
    val logger = KotlinLogging.logger {}

    /**
     * List of path prefixes which aren't subject to tenantization.
     */
    var blacklist: List<String> = emptyList()
    var dataExtensions: List<String> = emptyList()

    fun isBlacklisted(path: String): Boolean {
        return blacklist.any { fragment -> path.startsWith(fragment) } ||
            dataExtensions.any { path.endsWith(it) }
    }
}

val Blacklist = createApplicationPlugin(name = "Blacklist", ::BlacklistConfiguration) {
    val logger = pluginConfig.logger

    onCall { call ->
        val path = call.request.path()

        if (call.attributes.getOrNull(StudioDeploymentKey) != null) {
            call.attributes.put(BlacklistedKey, false)
        } else if (pluginConfig.isBlacklisted(path)) {
            logger.trace { "Matched $path to blacklist" }
            call.attributes.put(BlacklistedKey, true)
        } else {
            call.attributes.put(BlacklistedKey, false)
        }
    }
}
