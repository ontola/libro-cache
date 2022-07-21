package tools.empathy.libro.server.plugins

import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.config.ApplicationConfig
import io.ktor.util.AttributeKey
import tools.empathy.url.rebase
import kotlin.properties.Delegates

data class Service(
    val name: String,
    val match: Regex,
    val url: String,
    val bulk: Boolean,
)

data class Services(val services: List<Service>) {
    fun route(path: String): Url {
        val service = services.find { service -> service.match.containsMatchIn(path) }
            ?: throw IllegalStateException("No service matched")

        return Url(service.url).rebase(path)
    }

    fun resolve(path: String): Service {
        return services.find { service -> service.match.containsMatchIn(path) }
            ?: throw IllegalStateException("No service matched")
    }

    companion object {
        fun default(config: ServiceRegistryConfiguration): Services = Services(defaultServices(config))

        private fun defaultServices(c: ServiceRegistryConfiguration): List<Service> = listOf(
            Service(c.emailServiceName, c.emailServiceMatcher, c.emailServiceUrl, c.emailServiceBulk),
            Service(c.tokenServiceName, c.tokenServiceMatcher, c.tokenServiceUrl, c.tokenServiceBulk),
            // Default goes last
            Service(c.dataServiceName, c.dataServiceMatcher, c.dataServiceUrl, c.dataServiceBulk),
        )
    }
}

class ServiceRegistryConfiguration {
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

val ServiceRegistry = createApplicationPlugin(name = "ServiceRegistry", ::ServiceRegistryConfiguration) {
    val services = Services.default(pluginConfig)

    onCall { call ->
        call.attributes.put(ServiceRegistryKey, services)
    }
}

private val ServiceRegistryKey = AttributeKey<Services>("ServiceRegistryKey")

internal val ApplicationCall.services: Services
    get() = attributes.getOrNull(ServiceRegistryKey) ?: reportMissingRegistry()

private fun reportMissingRegistry(): Nothing {
    throw ServiceRegistryNotYetConfiguredException()
}
class ServiceRegistryNotYetConfiguredException :
    IllegalStateException("Service registry is not yet ready: you are asking it to early before the ServiceRegistry feature.")
