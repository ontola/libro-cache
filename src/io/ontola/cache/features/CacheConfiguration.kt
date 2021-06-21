package io.ontola.cache.features

import com.bugsnag.Bugsnag
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.config.ApplicationConfig
import io.ktor.util.AttributeKey
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.printStack
import io.lettuce.core.RedisURI

data class SessionsConfig(
    /**
     * The secret used for verifying session signatures.
     */
    val sessionSecret: String,
    /**
     * Token used to encrypt the session JWTs
     */
    val jwtEncryptionToken: String,
    /**
     * The client id to accept requests from.
     */
    val clientId: String,
    /**
     * Name of the legacy koa cookie that holds the session id
     */
    val cookieNameLegacy: String = "koa:sess",
    /**
     * Name of the legacy koa cookie that holds the session signature
     */
    val signatureNameLegacy: String = "koa:sess.sig",
)

data class RedisConfig(
    /**
     * The redis uri where the cache writes resources to.
     */
    val uri: RedisURI,

    /**
     * Root prefix to prepend to all keys.
     */
    val rootPrefix: String? = null,

    /**
     * Cache-specific prefix to append to the [rootPrefix] but before all other keys.
     */
    val cachePrefix: String? = "cache",

    /**
     * Key part for cache entries.
     */
    val cacheEntryPrefix: String? = "entry",

    /**
     * Separator to use inbetween key parts
     * https://redis.io/topics/data-types-intro#redis-keys
     */
    val separator: String = ":",
    /**
     * The channel where invalidation messages will be broadcast.
     */
    val invalidationChannel: String,
    /**
     * The group used when reading from the [invalidationChannel].
     */
    val invalidationGroup: String,
)

data class CacheConfig(
    /**
     * Configuration relating to session management
     */
    val sessions: SessionsConfig,
    /**
     * Configuration relating to cache data storage
     */
    val redis: RedisConfig,
    /**
     * The redis uri where libro writes sessions to.
     */
    val libroRedisURI: RedisURI,
    /**
     * Properties for other services in the environment
     */
    val services: ApplicationConfig,
    /**
     * The language to default to when.
     */
    val defaultLanguage: String,
    /**
     * Whether the invalidator should be running.
     */
    val enableInvalidator: Boolean,
    /**
     * Key of the error reporting service.
     */
    val reportingKey: String? = null,
    /**
     * The amount of time after which cache entries should expire in seconds.
     * Omit to disable expiration.
     */
    val cacheExpiration: Long? = null,
) {
    private val reportingService: Bugsnag? = if (reportingKey.isNullOrBlank()) {
        println("No reporting key")
        null
    } else {
        Bugsnag(reportingKey)
    }

    companion object {
        @KtorExperimentalAPI
        fun fromEnvironment(config: ApplicationConfig, testing: Boolean): CacheConfig {

            val cacheConfig = config.config("cache")
            val cacheSession = cacheConfig.config("session")

            val services = cacheConfig.config("services")

            val sessionsConfig = if (testing) {
                SessionsConfig(
                    sessionSecret = "",
                    jwtEncryptionToken = "",
                    clientId = "0",
                )
            } else {
                SessionsConfig(
                    sessionSecret = cacheSession.property("secret").getString(),
                    jwtEncryptionToken = cacheSession.property("jwtEncryptionToken").getString(),
                    clientId = services.config("oidc").property("clientId").getString(),
                )
            }

            val (libroRedisURI, redisConfig) = if (testing) {
                // TODO: in-memory redis

                val libroRedisDb = 0
                val testRedisURI = RedisURI.create("redis://redis")

                val libroRedisURI = testRedisURI.apply {
                    database = libroRedisDb
                }

                val redisConfig = RedisConfig(
                    uri = testRedisURI,
                    invalidationChannel = "invalidationChannel",
                    invalidationGroup = "testGroup",
                )

                Pair(libroRedisURI, redisConfig)
            } else {
                val redisConfigProp = services.config("redis")
                val redisHost = redisConfigProp.property("host").getString()
                val redisPort = redisConfigProp.property("port").getString().toInt()
                val redisDb = redisConfigProp.property("db").getString().toInt()
                val libroRedisDb = redisConfigProp.property("libroDb").getString().toInt()

                val redisURI = RedisURI.create(redisHost, redisPort).apply {
                    database = redisDb
                }
                val libroRedisURI = RedisURI.create(redisHost, redisPort).apply {
                    database = libroRedisDb
                }

                val redisConfig = RedisConfig(
                    uri = redisURI,
                    invalidationChannel = redisConfigProp.property("invalidationChannel").getString(),
                    invalidationGroup = redisConfigProp.property("invalidationGroup").getString(),
                )

                Pair(libroRedisURI, redisConfig)
            }

            val defaultLanguage = if (testing) {
                "en"
            } else {
                cacheConfig.property("defaultLanguage").getString()
            }

            return CacheConfig(
                sessions = sessionsConfig,
                libroRedisURI = libroRedisURI,
                redis = redisConfig,
                services = services,
                defaultLanguage = defaultLanguage,
                enableInvalidator = true, // TODO
                reportingKey = cacheConfig.propertyOrNull("reportingKey")?.toString(),
                cacheExpiration = cacheConfig.propertyOrNull("cacheExpiration")?.toString()?.toLongOrNull(),
            )
        }
    }

    /**
     * Prints the message and notifies the reporting service if available.
     */
    fun notify(e: Exception) {
        if (reportingService == null) {
            println("No reporting service")
            println(e.message)
            e.printStack()
        } else {
            println(e.message)
            reportingService.notify(e)
        }
    }
}

/**
 * Application-broad feature.
 */
class CacheConfiguration {
    class Configuration {
        lateinit var config: CacheConfig
    }

    // Implements ApplicationFeature as a companion object.
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, CacheConfiguration> {
        override val key = AttributeKey<CacheConfiguration>("CacheConfiguration")

        // Code to execute when installing the feature.
        @KtorExperimentalAPI
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): CacheConfiguration {
            val configuration = Configuration().apply(configure)
            val feature = CacheConfiguration()
            pipeline.attributes.put(CacheConfigurationKey, configuration.config)

            // Intercept a pipeline.
            pipeline.intercept(ApplicationCallPipeline.Features) {
                this.call.attributes.put(CacheConfigurationKey, configuration.config)
            }
            return feature
        }
    }
}

private val CacheConfigurationKey = AttributeKey<CacheConfig>("CacheConfigurationKey")

internal val ApplicationCallPipeline.cacheConfig: CacheConfig
    get() = attributes.getOrNull(CacheConfigurationKey) ?: reportMissingRegistry()

private fun reportMissingRegistry(): Nothing {
    throw CacheConfigurationNotYetConfiguredException()
}
class CacheConfigurationNotYetConfiguredException :
    IllegalStateException("Cache configuration is not yet ready: you are asking it to early before the CacheConfiguration feature.")
