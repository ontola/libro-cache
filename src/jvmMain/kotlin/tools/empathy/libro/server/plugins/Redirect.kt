package tools.empathy.libro.server.plugins

import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.plugins.origin
import io.ktor.server.request.path
import io.ktor.server.response.respondRedirect

private const val redirectPrefix = "redirect"

fun removeTrailingSlash(value: String) = if (value.endsWith('/')) value.dropLast(1) else value

class RedirectConfiguration {
    lateinit var storage: Storage

    fun complete(storage: Storage) {
        if (!this::storage.isInitialized) this.storage = storage
    }
}

/**
 * Redirect certain prefixes to new locations.
 * Useful when maintaining platforms over time.
 */
val Redirect = createApplicationPlugin(name = "Redirect", ::RedirectConfiguration) {
    pluginConfig.complete(application.persistentStorage)

    onCall { call ->
        val uri = removeTrailingSlash("${call.request.origin.host}${call.request.path()}")
        val redirectLocation = pluginConfig.storage.getString(redirectPrefix, uri)

        if (redirectLocation != null)
            call.respondRedirect(redirectLocation, true)
    }
}
