package tools.empathy.libro.server.plugins

import io.ktor.http.Url
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.plugins.origin
import io.ktor.server.request.path
import io.ktor.server.response.respondRedirect
import tools.empathy.url.absolutize
import tools.empathy.url.appendPath
import tools.empathy.url.asHrefString

class RedirectConfiguration {
    lateinit var storage: Storage

    fun complete(storage: Storage) {
        if (!this::storage.isInitialized) this.storage = storage
    }
}

enum class RedirectKeys {
    Exact,
    Prefix,
}

/**
 * Redirect certain prefixes to new locations.
 * Useful when maintaining platforms over time.
 */
val Redirect = createApplicationPlugin(name = "Redirect", ::RedirectConfiguration) {
    pluginConfig.complete(application.persistentStorage)

    onCall { call ->
        val uri = Url("${call.request.origin.scheme}://${call.request.origin.host}${call.request.path()}")
        val exactRedirect = pluginConfig.storage.getHashValue(LookupKeys.Redirect.name, RedirectKeys.Exact.name, hashKey = uri.asHrefString)
        if (exactRedirect != null)
            call.respondRedirect(exactRedirect, true)
        else {
            val prefixRedirects = pluginConfig.storage.getHash(LookupKeys.Redirect.name, RedirectKeys.Prefix.name)

            val key = prefixRedirects.keys.firstOrNull { matchRedirect(uri, Url(it)) }

            if (key != null) {
                val value = prefixRedirects[key]!!
                val redirectLocation = rewriteLocation(uri, Url(key), Url(value))

                call.respondRedirect(redirectLocation, true)
            }
        }
    }
}

private fun matchRedirect(location: Url, match: Url): Boolean {
    return location == match || location.toString().startsWith(match.toString())
}

private fun rewriteLocation(location: Url, from: Url, to: Url): String {
    return to.appendPath(from.absolutize(location)).asHrefString
}
