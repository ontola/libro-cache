package io.ontola.cache.features

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.config.ApplicationConfig
import io.ktor.util.AttributeKey
import io.ktor.util.KtorExperimentalAPI

data class Service(
    val name: String,
    val match: Regex,
    val url: String,
)

data class Services(val services: List<Service>) {
    fun route(path: String): String {
        val service = services.find { service -> service.match.containsMatchIn(path) }
            ?: throw IllegalStateException("No service matched")

        return "${service.url}$path"
    }

    companion object {
        fun default(config: ServiceRegistry.Configuration): Services = Services(defaultServices(config))

        @KtorExperimentalAPI
        private fun defaultServices(c: ServiceRegistry.Configuration): List<Service> = listOf(
//            Service(c.cacheServiceName, c.cacheServiceMatcher, c.cacheServiceUrl),
            Service(c.emailServiceName, c.emailServiceMatcher, c.emailServiceUrl),
            Service(c.tokenServiceName, c.tokenServiceMatcher, c.tokenServiceUrl),
            // Default goes last
            Service(c.dataServiceName, c.dataServiceMatcher, c.dataServiceUrl),
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

//        lateinit var cacheServiceUrl: String
//        lateinit var cacheServiceName: String
//        lateinit var cacheServiceMatcher: Regex

        lateinit var emailServiceUrl: String
        lateinit var emailServiceName: String
        lateinit var emailServiceMatcher: Regex

        lateinit var tokenServiceUrl: String
        lateinit var tokenServiceName: String
        lateinit var tokenServiceMatcher: Regex


        fun initFromTest(config: ApplicationConfig) {
            this.config = config

            dataServiceUrl = "https://data"
            dataServiceName = "data"
            dataServiceMatcher = Regex(".*")
            emailServiceUrl = "https://email"
            emailServiceName = "email"
            emailServiceMatcher = Regex("^/email/")
            tokenServiceUrl = "https://token"
            tokenServiceName = "token"
            tokenServiceMatcher = Regex("^(/\\w+)?/tokens")
        }

        fun initFrom(config: ApplicationConfig) {
            this.config = config

            dataServiceUrl = config.config("data").property("url").getString()
            dataServiceName = config.config("data").property("name").getString()
            dataServiceMatcher = Regex(config.config("data").property("matcher").getString())
//            cacheServiceUrl = config.config("cache").property("url").getString()
//            cacheServiceName = config.config("cache").property("name").getString()
//            cacheServiceMatcher = Regex(config.config("cache").property("matcher").getString())
            emailServiceUrl = config.config("email").property("url").getString()
            emailServiceName = config.config("email").property("name").getString()
            emailServiceMatcher = Regex(config.config("email").property("matcher").getString())
            tokenServiceUrl = config.config("token").property("url").getString()
            tokenServiceName = config.config("token").property("name").getString()
            tokenServiceMatcher = Regex(config.config("token").property("matcher").getString())
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
