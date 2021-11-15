package io.ontola.cache.plugins

import com.bugsnag.Bugsnag
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.config.ApplicationConfig
import io.ktor.util.AttributeKey
import io.ktor.utils.io.printStack
import io.lettuce.core.RedisURI
import io.ontola.cache.createClient
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

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
     * The id to identify this client.
     */
    val clientId: String,
    /**
     * The secret to identify this client.
     */
    val clientSecret: String,
    /**
     * TODO: Refactor away
     */
    val oAuthToken: String,
    /**
     * Name of the legacy koa cookie that holds the session id
     */
    val cookieNameLegacy: String = "koa:sess",
    /**
     * Name of the legacy koa cookie that holds the session signature
     */
    val signatureNameLegacy: String = "koa:sess.sig",
    /**
     * The url of the OIDC identity provider
     */
    val oidcUrl: String,
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
    val cacheEntryPrefix: String = "entry",

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

data class CacheConfig @OptIn(ExperimentalTime::class) constructor(
    /**
     * Whether the application is running in test mode.
     */
    val testing: Boolean,
    /**
     * Configuration relating to session management.
     */
    val sessions: SessionsConfig,
    /**
     * Configuration relating to cache data storage.
     */
    val redis: RedisConfig,
    /**
     * The redis uri where libro writes sessions to.
     */
    val libroRedisURI: RedisURI,
    /**
     * The redis uri used for streaming between services.
     */
    val streamRedisURI: RedisURI,
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
     * The amount of seconds after which cache entries should expire.
     * Omit to disable expiration.
     */
    val cacheExpiration: Long? = null,
    /**
     * The amount of seconds after which tenant finder lookups should expire.
     * Set to zero to disable caching.
     */
    val tenantExpiration: Long = Duration.minutes(10).inWholeSeconds,
    /**
     * Client to use for requests to external systems.
     */
    val client: HttpClient,
    val serializer: Json = Json {
        encodeDefaults = true
        isLenient = false
        ignoreUnknownKeys = false
    },
) {
    private val logger = KotlinLogging.logger {}

    private val reportingService: Bugsnag? = if (reportingKey.isNullOrBlank()) {
        logger.warn("No reporting key")
        null
    } else {
        Bugsnag(reportingKey)
    }

    companion object {
        fun fromEnvironment(
            config: ApplicationConfig,
            testing: Boolean,
            client: HttpClient = createClient(),
        ): CacheConfig {

            val cacheConfig = config.config("cache")
            val cacheSession = cacheConfig.config("session")

            val services = cacheConfig.config("services")

            val sessionsConfig = if (testing) {
                SessionsConfig(
                    sessionSecret = "",
                    jwtEncryptionToken = "",
                    clientId = "0",
                    clientSecret = "",
                    oidcUrl = "",
                    oAuthToken = "",
                )
            } else {
                val oidcConfig = services.config("oidc")

                SessionsConfig(
                    sessionSecret = cacheSession.property("secret").getString(),
                    jwtEncryptionToken = cacheSession.property("jwtEncryptionToken").getString(),
                    clientId = oidcConfig.property("clientId").getString(),
                    clientSecret = oidcConfig.property("clientSecret").getString(),
                    oAuthToken = oidcConfig.property("oAuthToken").getString(),
                    oidcUrl = oidcConfig.property("url").getString(),
                )
            }

            val (libroRedisURI, streamRedisURI, redisConfig) = if (testing) {
                // TODO: in-memory redis

                val testRedisURI = RedisURI.create("redis://redis")

                val libroRedisDb = 0
                val libroRedisURI = testRedisURI.apply {
                    database = libroRedisDb
                }

                val streamRedisDb = 0
                val streamRedisURI = testRedisURI.apply {
                    database = streamRedisDb
                }

                val redisConfig = RedisConfig(
                    uri = testRedisURI,
                    invalidationChannel = "invalidationChannel",
                    invalidationGroup = "testGroup",
                )

                Triple(libroRedisURI, streamRedisURI, redisConfig)
            } else {
                val redisConfigProp = services.config("redis")
                val redisHost = redisConfigProp.property("host").getString()
                val redisPort = redisConfigProp.property("port").getString().toInt()
                val redisDb = redisConfigProp.property("db").getString().toInt()
                val redisUsername = redisConfigProp.propertyOrNull("username")?.getString()
                val redisPassword = redisConfigProp.propertyOrNull("password")?.getString()?.toCharArray()
                val redisSsl = redisConfigProp.propertyOrNull("ssl")?.getString()?.toBoolean()
                val libroRedisDb = redisConfigProp.property("libroDb").getString().toInt()
                val streamRedisDb = redisConfigProp.property("streamDb").getString().toInt()
                fun redisUrl(db: Int) = RedisURI
                    .builder()
                    .withHost(redisHost)
                    .withPort(redisPort)
                    .withDatabase(db)
                    .withSsl(redisSsl ?: false)
                    .apply {
                        if (redisPassword !== null) {
                            withPassword(redisPassword)
                        }
                        if (redisUsername !== null && redisPassword !== null) {
                            withAuthentication(redisUsername, redisPassword)
                        }
                    }
                    .build()

                val redisURI = redisUrl(redisDb)
                val libroRedisURI = redisUrl(libroRedisDb)
                val streamRedisURI = redisUrl(streamRedisDb)

                val redisConfig = RedisConfig(
                    uri = redisURI,
                    invalidationChannel = redisConfigProp.property("invalidationChannel").getString(),
                    invalidationGroup = redisConfigProp.property("invalidationGroup").getString(),
                )

                Triple(libroRedisURI, streamRedisURI, redisConfig)
            }

            val defaultLanguage = if (testing) {
                "en"
            } else {
                cacheConfig.property("defaultLanguage").getString()
            }

            return CacheConfig(
                testing = testing,
                sessions = sessionsConfig,
                libroRedisURI = libroRedisURI,
                streamRedisURI = streamRedisURI,
                redis = redisConfig,
                services = services,
                defaultLanguage = defaultLanguage,
                enableInvalidator = true, // TODO
                reportingKey = cacheConfig.propertyOrNull("reportingKey")?.toString(),
                cacheExpiration = cacheConfig.propertyOrNull("cacheExpiration")?.toString()?.toLongOrNull(),
                client = client,
            )
        }
    }

    /**
     * Prints the message and notifies the reporting service if available.
     */
    fun notify(e: Exception) {
        logger.error(e.message)

        if (reportingService == null) {
            logger.warn("No reporting service")
            e.printStack()
        } else {
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

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, CacheConfiguration> {
        override val key = AttributeKey<CacheConfiguration>("CacheConfiguration")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): CacheConfiguration {
            val configuration = Configuration().apply(configure)
            val feature = CacheConfiguration()
            pipeline.attributes.put(CacheConfigurationKey, configuration.config)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                call.attributes.put(CacheConfigurationKey, configuration.config)
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
