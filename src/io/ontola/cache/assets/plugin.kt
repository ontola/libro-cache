package io.ontola.cache.assets

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.feature
import io.ktor.util.AttributeKey
import io.ontola.cache.plugins.CacheSession
import io.ontola.cache.plugins.cacheConfig

class Assets(private val configuration: Configuration) {
    class Configuration

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, Assets> {
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
    feature(CacheSession) // ensure the feature is installed
    throw AssetsNotYetConfiguredException()
}

class AssetsNotYetConfiguredException :
    IllegalStateException("Assets is not yet ready: you are asking it to early before the Assets feature.")
