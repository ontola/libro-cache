package tools.empathy.libro.server.configuration

import com.bugsnag.Bugsnag
import io.ktor.client.HttpClient
import io.ktor.http.Url
import io.ktor.server.config.ApplicationConfig
import io.ktor.utils.io.printStack
import io.lettuce.core.RedisURI
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import tools.empathy.libro.server.createClient
import tools.empathy.libro.server.csp.CSPReportException
import tools.empathy.libro.server.plugins.Versions

data class LibroConfig constructor(
    /**
     * Whether the application is running in test mode.
     */
    val testing: Boolean,
    val env: String = if (testing) "testing" else System.getenv("KTOR_ENV") ?: "production",
    val port: Int,
    /**
     * Configuration relating to client bundles.
     */
    val bundles: BundlesConfig,
    /**
     * Configuration relating to the management panel.
     */
    val management: ManagementConfig,
    /**
     * Configuration relating to session management.
     */
    val sessions: SessionsConfig,
    /**
     * Configuration relating to cache data storage.
     */
    val redis: RedisConfig,
    /**
     * The redis uri where to write persistent data to (sessions, settings, documents).
     */
    val persistentRedisURI: RedisURI,
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
    val studio: StudioConfig,
    /**
     * Whether the invalidator module should be running.
     * @see tools.empathy.libro.server.invalidator.module
     */
    val enableInvalidator: Boolean,
    val maps: MapsConfig? = null,
    /**
     * Key of the server error reporting service.
     */
    val serverReportingKey: String? = null,
    /**
     * Key of the client error reporting service.
     */
    val clientReportingKey: String? = null,
    /**
     * The amount of seconds after which cache entries should expire.
     * Omit to disable expiration.
     */
    val cacheExpiration: Long? = null,
    /**
     * Client to use for requests to external systems.
     */
    val clientOverride: HttpClient? = null,
    val serializer: Json = Json {
        prettyPrint = env == "development"
        encodeDefaults = true
        isLenient = false
        ignoreUnknownKeys = false
    },
    val serverVersion: String?,
    val clientVersion: String?,
) {
    private val logger = KotlinLogging.logger {}

    val client: HttpClient = clientOverride ?: createClient(env == "production")

    private val reportingService: Bugsnag? = if (serverReportingKey.isNullOrBlank()) {
        logger.warn("No reporting key")
        null
    } else {
        Bugsnag(serverReportingKey).apply {
            setAppVersion(this@LibroConfig.serverVersion)
            setAppType("libro-server")
            setReleaseStage(this@LibroConfig.env)
            addCallback {
                when (val exception = it.exception!!) {
                    is CSPReportException -> {
                        val report = exception.report

                        it.addToTab("cspReport", "blockedUri", report.blockedUri)
                        it.addToTab("cspReport", "columnNumber", report.columnNumber)
                        it.addToTab("cspReport", "documentUri", report.documentUri)
                        it.addToTab("cspReport", "lineNumber", report.lineNumber)
                        it.addToTab("cspReport", "originalPolicy", report.originalPolicy)
                        it.addToTab("cspReport", "referrer", report.referrer)
                        it.addToTab("cspReport", "scriptSample", report.scriptSample)
                        it.addToTab("cspReport", "sourceFile", report.sourceFile)
                        it.addToTab("cspReport", "violatedDirective", report.violatedDirective)
                    }
                    else -> Unit
                }
            }
        }
    }

    inline val envKind get() = this.env
    inline val isDev get() = envKind == "development"
    inline val isTesting get() = envKind == "testing"
    inline val isProd get() = envKind == "production"

    companion object {
        fun fromEnvironment(
            config: ApplicationConfig,
            testing: Boolean,
            client: HttpClient? = null,
        ): LibroConfig {
            val libroConfig = config.config("libro")

            val (persistentRedisURI, streamRedisURI, redisConfig) = redisConfig(libroConfig, testing)

            return LibroConfig(
                bundles = bundlesConfig(libroConfig, testing),
                port = config.config("ktor").config("deployment").property("port").getString().toInt(),
                testing = testing,
                management = managementConfig(libroConfig, testing),
                sessions = sessionsConfig(libroConfig, testing),
                persistentRedisURI = persistentRedisURI,
                streamRedisURI = streamRedisURI,
                redis = redisConfig,
                services = libroConfig.config("services"),
                defaultLanguage = defaultLanguage(libroConfig, testing),
                studio = studioConfig(config),
                enableInvalidator = true, // TODO
                maps = mapsConfig(libroConfig, testing),
                serverReportingKey = libroConfig.config("reporting").propertyOrNull("serverReportingKey")?.getString(),
                clientReportingKey = libroConfig.config("reporting").propertyOrNull("clientReportingKey")?.getString(),
                cacheExpiration = libroConfig.propertyOrNull("cacheExpiration")?.toString()?.toLongOrNull(),
                clientOverride = client,
                serverVersion = Versions.ServerVersion,
                clientVersion = Versions.ClientVersion,
            )
        }

        private fun defaultLanguage(
            libroConfig: ApplicationConfig,
            testing: Boolean,
        ): String = if (testing) {
            "en"
        } else {
            libroConfig.property("defaultLanguage").getString()
        }

        private fun bundlesConfig(
            libroConfig: ApplicationConfig,
            testing: Boolean,
        ): BundlesConfig {
            val bundlesConfig = libroConfig.config("bundle")

            return if (testing) {
                BundlesConfig(
                    publicFolder = "f_assets",
                    defaultBundle = "main",
                )
            } else {
                BundlesConfig(
                    publicFolder = bundlesConfig.property("publicFolder").getString(),
                    defaultBundle = bundlesConfig.property("defaultBundle").getString(),
                )
            }
        }

        private fun redisConfig(
            libroConfig: ApplicationConfig,
            testing: Boolean,
        ): Triple<RedisURI, RedisURI, RedisConfig> = if (testing) {
            // TODO: in-memory redis

            val testRedisURI = RedisURI.create("redis://redis")

            val persistentRedisDb = 0
            val persistentRedisURI = testRedisURI.apply {
                database = persistentRedisDb
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

            Triple(persistentRedisURI, streamRedisURI, redisConfig)
        } else {
            val redisConfigProp = libroConfig.config("services").config("redis")
            val redisHost = redisConfigProp.property("host").getString()
            val redisPort = redisConfigProp.property("port").getString().toInt()
            val redisDb = redisConfigProp.property("db").getString().toInt()
            val redisUsername = redisConfigProp.propertyOrNull("username")?.getString()
            val redisPassword = redisConfigProp.propertyOrNull("password")?.getString()?.toCharArray()
            val redisSsl = redisConfigProp.propertyOrNull("ssl")?.getString()?.toBoolean()
            val persistentRedisDb = redisConfigProp.property("persistentDb").getString().toInt()
            val streamRedisDb = redisConfigProp.property("streamDb").getString().toInt()
            fun redisUrl(db: Int) = RedisURI.builder()
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
            val persistentRedisURI = redisUrl(persistentRedisDb)
            val streamRedisURI = redisUrl(streamRedisDb)

            val redisConfig = RedisConfig(
                uri = redisURI,
                invalidationChannel = redisConfigProp.property("invalidationChannel").getString(),
                invalidationGroup = redisConfigProp.property("invalidationGroup").getString(),
            )

            Triple(persistentRedisURI, streamRedisURI, redisConfig)
        }

        private fun mapsConfig(
            libroConfig: ApplicationConfig,
            testing: Boolean,
        ): MapsConfig? {
            if (testing) {
                return null
            }

            val username = libroConfig.config("maps").propertyOrNull("username")?.getString() ?: return null
            val key = libroConfig.config("maps").propertyOrNull("key")?.getString() ?: return null

            return MapsConfig(
                username = username,
                key = key,
            )
        }
        private fun managementConfig(libroConfig: ApplicationConfig, testing: Boolean): ManagementConfig {
            if (testing) {
                return ManagementConfig(
                    origin = Url("https://localhost"),
                )
            }
            val managementConfig = libroConfig.config("management")

            return ManagementConfig(
                origin = Url(managementConfig.property("origin").getString()),
            )
        }

        private fun sessionsConfig(
            libroConfig: ApplicationConfig,
            testing: Boolean,
        ): SessionsConfig {
            val services = libroConfig.config("services")
            val libroSession = libroConfig.config("session")

            return if (testing) {
                SessionsConfig.forTesting()
            } else {
                val oidcConfig = services.config("oidc")

                SessionsConfig(
                    sessionSecret = libroSession.property("secret").getString(),
                    jwtEncryptionToken = libroSession.property("jwtEncryptionToken").getString(),
                    clientName = oidcConfig.property("clientName").getString(),
                    oidcUrl = Url(oidcConfig.property("url").getString()),
                )
            }
        }

        private fun studioConfig(config: ApplicationConfig): StudioConfig = StudioConfig(
            skipAuth = config.config("studio").property("skipAuth").getString().toBoolean(),
            domain = config.config("studio").property("domain").getString(),
        )
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

    fun print() {
        logger.info {
            val logLevel = when {
                logger.isTraceEnabled -> "Trace"
                logger.isDebugEnabled -> "Debug"
                logger.isInfoEnabled -> "Info"
                logger.isWarnEnabled -> "Warn"
                logger.isErrorEnabled -> "Error"
                else -> "Unknown"
            }

            arrayOf(
                "Config",
                "Env: $envKind",
                "Testing mode: $testing",
                "Log level: $logLevel",
                "Default lang: $defaultLanguage",
                "Invalidator: $enableInvalidator",
                "Cache exp: ${if (cacheExpiration == null) "never" else "$cacheExpiration seconds"}",
                "Server reporting: $serverReportingKey",
                "Client reporting: $clientReportingKey",
            ).joinToString(", ")
        }
    }
}
