package tools.empathy.libro.server.configuration

import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.createApplicationPlugin
import io.ktor.util.AttributeKey

class Configuration {
    lateinit var config: LibroConfig
}

private val libroConfigurationKey = AttributeKey<LibroConfig>("LibroConfiguration")

val LibroConfiguration = createApplicationPlugin(name = "LibroConfiguration", ::Configuration) {
    application.attributes.put(libroConfigurationKey, pluginConfig.config)

    onCall {
        it.attributes.put(libroConfigurationKey, pluginConfig.config)
    }
}

internal val ApplicationCallPipeline.libroConfig: LibroConfig
    get() = attributes.getOrNull(libroConfigurationKey) ?: reportMissingRegistry()

private fun reportMissingRegistry(): Nothing {
    throw LibroConfigurationNotYetConfiguredException()
}
class LibroConfigurationNotYetConfiguredException :
    IllegalStateException("Libro configuration is not yet ready: you are asking it to early before the LibroConfiguration feature.")
