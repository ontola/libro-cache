package io.ontola.cache.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.header
import io.ktor.util.AttributeKey
import io.ontola.cache.util.preferredLanguage

class LanguageNegotiationConfiguration {
    lateinit var defaultLanguage: String
}

val LanguageNegotiation = createApplicationPlugin(
    "LanguageNegotiation",
    { LanguageNegotiationConfiguration() },
) {

    onCall { call ->
        call.attributes.put(
            LanguageNegotiationKey,
            call.request.header(HttpHeaders.AcceptLanguage)?.preferredLanguage()
                ?: pluginConfig.defaultLanguage
        )
    }
}

private val LanguageNegotiationKey = AttributeKey<String>("LanguageNegotiationKey")

private val PreferredLanguageKey = AttributeKey<String>("PreferredLanguage")

internal val ApplicationCall.language: String
    get() = attributes.getOrNull(PreferredLanguageKey)
        ?: attributes.getOrNull(LanguageNegotiationKey)
        ?: reportMissingRegistry()

internal fun ApplicationCall.setPreferredLanguage(language: String?) {
    if (language != null)
        attributes.put(PreferredLanguageKey, language)
    else
        attributes.remove(PreferredLanguageKey)
}

private fun reportMissingRegistry(): Nothing {
    throw LanguageNegotiationNotYetConfiguredException()
}
class LanguageNegotiationNotYetConfiguredException :
    IllegalStateException("Cache configuration is not yet ready: you are asking it to early before the LanguageNegotiation feature.")
