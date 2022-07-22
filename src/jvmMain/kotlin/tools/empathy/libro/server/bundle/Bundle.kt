@file:JvmName("BundlesKt")

package tools.empathy.libro.server.bundle

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.util.AttributeKey
import tools.empathy.libro.server.configuration.libroConfig

/**
 * Provides info on the client (JS) bundle.
 */
val Bundle = createApplicationPlugin(name = "Bundle") {
    val bundles = loadBundleManifests(application.libroConfig)

    onCall { call ->
        call.attributes.put(BundleKey, bundles)
    }
}

private val BundleKey = AttributeKey<Bundles>("BundleKey")

internal val ApplicationCall.bundles: Bundles
    get() = attributes.getOrNull(BundleKey) ?: reportMissingBundles()

private fun reportMissingBundles(): Nothing {
    throw BundleNotYetConfiguredException()
}

class BundleNotYetConfiguredException :
    IllegalStateException("Bundle is not yet ready: you are asking it to early before the Bundle feature.")
