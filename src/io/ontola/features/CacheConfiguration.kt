package io.ontola.features

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.config.ApplicationConfig
import io.ktor.util.AttributeKey
import io.ktor.util.KtorExperimentalAPI

data class CacheConfig(
    val sessionSecret: String?,
) {
    companion object {
        @KtorExperimentalAPI
        fun fromEnvironment(config: ApplicationConfig): CacheConfig {

            val cacheConfig = config.config("cache")
            val cacheSession = cacheConfig.config("session")

            return CacheConfig(
                sessionSecret = cacheSession.propertyOrNull("secret")?.getString()
            )
        }
    }
}

class CacheConfiguration {
    class Configuration {
        lateinit var config: ApplicationConfig
    }

    // Implements ApplicationFeature as a companion object.
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, CacheConfiguration> {
        override val key = AttributeKey<CacheConfiguration>("CacheConfiguration")

        // Code to execute when installing the feature.
        @KtorExperimentalAPI
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): CacheConfiguration {
            val configuration = Configuration().apply(configure)
            val feature = CacheConfiguration()
            val config = CacheConfig.fromEnvironment(configuration.config)

            // Intercept a pipeline.
            pipeline.intercept(ApplicationCallPipeline.Features) {
                this.call.attributes.put(CacheConfigurationKey, config)
            }
            return feature
        }
    }
}

private val CacheConfigurationKey = AttributeKey<CacheConfig>("CacheConfigurationKey")

internal val ApplicationCall.cacheConfig: CacheConfig
    get() = attributes.getOrNull(CacheConfigurationKey) ?: reportMissingRegistry()

private fun ApplicationCall.reportMissingRegistry(): Nothing {
    application.feature(CacheConfiguration) // ensure the feature is installed
    throw CacheConfigurationNotYetConfiguredException()
}
class CacheConfigurationNotYetConfiguredException :
    IllegalStateException("Service registry is not yet ready: you are asking it to early before the CacheConfiguration feature.")
