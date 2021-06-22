package io.ontola.cache.features

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.config.ApplicationConfig
import io.ktor.util.AttributeKey
import io.ktor.util.KtorExperimentalAPI
import kotlin.properties.Delegates

data class Service(
    val name: String,
    val match: Regex,
    val url: String,
    val bulk: Boolean,
)

data class Services(val services: List<Service>) {
    fun route(path: String): String {
        val service = services.find { service -> service.match.containsMatchIn(path) }
            ?: throw IllegalStateException("No service matched")

        return "${service.url}$path"
    }

    fun resolve(path: String): Service {
        return services.find { service -> service.match.containsMatchIn(path) }
            ?: throw IllegalStateException("No service matched")
    }

    companion object {
        fun default(config: ServiceRegistry.Configuration): Services = Services(defaultServices(config))

        @KtorExperimentalAPI
        private fun defaultServices(c: ServiceRegistry.Configuration): List<Service> = listOf(
//            Service(c.cacheServiceName, c.cacheServiceMatcher, c.cacheServiceUrl),
            Service(c.emailServiceName, c.emailServiceMatcher, c.emailServiceUrl, c.emailServiceBulk),
            Service(c.tokenServiceName, c.tokenServiceMatcher, c.tokenServiceUrl, c.tokenServiceBulk),
            // Default goes last
            Service(c.dataServiceName, c.dataServiceMatcher, c.dataServiceUrl, c.dataServiceBulk),
        )
    }
}

class ServiceRegistry(private val configuration: Configuration) {
    @KtorExperimentalAPI
    class Configuration {
        lateinit var config: ApplicationConfig

        lateinit var dataServiceUrl: String
        lateinit var dataServiceName: String
        lateinit var dataServiceMatcher: Regex
        var dataServiceBulk by Delegates.notNull<Boolean>()

//        lateinit var cacheServiceUrl: String
//        lateinit var cacheServiceName: String
//        lateinit var cacheServiceMatcher: Regex

        lateinit var emailServiceUrl: String
        lateinit var emailServiceName: String
        lateinit var emailServiceMatcher: Regex
        var emailServiceBulk by Delegates.notNull<Boolean>()

        lateinit var tokenServiceUrl: String
        lateinit var tokenServiceName: String
        lateinit var tokenServiceMatcher: Regex
        var tokenServiceBulk by Delegates.notNull<Boolean>()

        fun initFromTest(config: ApplicationConfig) {
            this.config = config

            dataServiceUrl = "https://data.local"
            dataServiceName = "data"
            dataServiceMatcher = Regex(".*")
            dataServiceBulk = true

            emailServiceUrl = "https://email.local"
            emailServiceName = "email"
            emailServiceMatcher = Regex("^/email/")
            emailServiceBulk = false

            tokenServiceUrl = "https://token.local"
            tokenServiceName = "token"
            tokenServiceMatcher = Regex("^(/\\w+)?/tokens")
            tokenServiceBulk = false
        }

        fun initFrom(config: ApplicationConfig) {
            this.config = config

            dataServiceUrl = config.config("data").property("url").getString()
            dataServiceName = config.config("data").property("name").getString()
            dataServiceMatcher = Regex(config.config("data").property("matcher").getString())
            dataServiceBulk = config.config("data").property("bulk").getString().toBoolean()

//            cacheServiceUrl = config.config("cache").property("url").getString()
//            cacheServiceName = config.config("cache").property("name").getString()
//            cacheServiceMatcher = Regex(config.config("cache").property("matcher").getString())
            emailServiceUrl = config.config("email").property("url").getString()
            emailServiceName = config.config("email").property("name").getString()
            emailServiceMatcher = Regex(config.config("email").property("matcher").getString())
            emailServiceBulk = config.config("email").property("bulk").getString().toBoolean()

            tokenServiceUrl = config.config("token").property("url").getString()
            tokenServiceName = config.config("token").property("name").getString()
            tokenServiceMatcher = Regex(config.config("token").property("matcher").getString())
            tokenServiceBulk = config.config("token").property("bulk").getString().toBoolean()

        }
    }

    // Implements ApplicationFeature as a companion object.
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, ServiceRegistry> {
        override val key = AttributeKey<ServiceRegistry>("ServiceRegistry")

        // Code to execute when installing the feature.
        @KtorExperimentalAPI
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): ServiceRegistry {
            val configuration = Configuration().apply(configure)
            val feature = ServiceRegistry(configuration)
            val services = Services.default(configuration)

            // Intercept a pipeline.
            pipeline.intercept(ApplicationCallPipeline.Features) {
                this.call.attributes.put(ServiceRegistryKey, services)
            }
            return feature
        }
    }
}

private val ServiceRegistryKey = AttributeKey<Services>("ServiceRegistryKey")

internal val ApplicationCall.services: Services
    get() = attributes.getOrNull(ServiceRegistryKey) ?: reportMissingRegistry()

private fun ApplicationCall.reportMissingRegistry(): Nothing {
    application.feature(ServiceRegistry) // ensure the feature is installed
    throw ServiceRegistryNotYetConfiguredException()
}
class ServiceRegistryNotYetConfiguredException :
    IllegalStateException("Service registry is not yet ready: you are asking it to early before the ServiceRegistry feature.")
