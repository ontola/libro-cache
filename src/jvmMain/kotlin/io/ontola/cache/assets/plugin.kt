package io.ontola.cache.assets

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.util.AttributeKey
import io.ontola.cache.plugins.cacheConfig

val Assets = createApplicationPlugin(name = "Assets") {
    val assets = loadAssetsManifests(application.cacheConfig)

    onCall { call ->
        call.attributes.put(AssetsKey, assets)
    }
}

private val AssetsKey = AttributeKey<AssetsManifests>("AssetsKey")

internal val ApplicationCall.assets: AssetsManifests
    get() = attributes.getOrNull(AssetsKey) ?: reportMissingAssets()

private fun reportMissingAssets(): Nothing {
    throw AssetsNotYetConfiguredException()
}

class AssetsNotYetConfiguredException :
    IllegalStateException("Assets is not yet ready: you are asking it to early before the Assets feature.")
