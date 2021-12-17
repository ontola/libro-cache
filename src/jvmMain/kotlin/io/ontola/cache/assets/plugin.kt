package io.ontola.cache.assets

import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.util.AttributeKey
import io.ontola.cache.plugins.CacheSession
import io.ontola.cache.plugins.cacheConfig

class Assets(private val configuration: Configuration) {
    class Configuration

    companion object Plugin : ApplicationPlugin<ApplicationCallPipeline, Configuration, Assets> {
        override val key = AttributeKey<Assets>("Assets")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Assets {
            val configuration = Configuration().apply(configure)
            val feature = Assets(configuration)
            val assets = loadAssetsManifests(pipeline.cacheConfig)
            pipeline.attributes.put(AssetsKey, assets)

            return feature
        }
    }
}

private val AssetsKey = AttributeKey<AssetsManifests>("AssetsKey")

internal val ApplicationCallPipeline.assets: AssetsManifests
    get() = attributes.getOrNull(AssetsKey) ?: reportMissingAssets()

private fun ApplicationCallPipeline.reportMissingAssets(): Nothing {
    plugin(CacheSession) // ensure the feature is installed
    throw AssetsNotYetConfiguredException()
}

class AssetsNotYetConfiguredException :
    IllegalStateException("Assets is not yet ready: you are asking it to early before the Assets feature.")
