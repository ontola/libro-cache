package tools.empathy.libro.server.sessions.oidc

import tools.empathy.libro.server.configuration.LibroConfig
import tools.empathy.libro.server.plugins.Storage

class OIDCSettingsManager(
    private val config: LibroConfig,
    storage: Storage,
) {
    private val oidcRegistry = OIDCSettingsRepository(storage)
    private val oidcClient = OIDCClient(config.client, config.sessions.clientName)

    suspend fun get(): OIDCServerSettings? {
        val origin = config.sessions.oidcUrl

        return oidcRegistry.getByOrigin(origin) ?: run {
            val oidcConfig = oidcClient.getConfiguration(origin) ?: return@run null
            println("providerLookup oidcConfig: $oidcConfig")
            val credentials = oidcClient.createApplication(oidcConfig, listOf(nativeRedirectUri))

            OIDCServerSettings(
                origin = origin,
                authorizeUrl = oidcConfig.authorizationEndpoint,
                accessTokenUrl = oidcConfig.tokenEndpoint,
                credentials = credentials,
            ).also {
                println("providerLookup persisting oidcConfig, $it")
                oidcRegistry.setByOrigin(origin, it)
            }
        }
    }
}
