package tools.empathy.libro.server.sessions.oidc

import io.ktor.http.Url
import mu.KotlinLogging
import tools.empathy.libro.server.configuration.LibroConfig
import tools.empathy.libro.server.plugins.Storage

class OIDCSettingsManager(
    private val config: LibroConfig,
    storage: Storage,
) {
    private val logger = KotlinLogging.logger {}
    private val oidcRegistry = OIDCSettingsRepository(storage)
    private val oidcClient = OIDCClient(config.client, config.sessions.clientName)

    suspend fun get(origin: Url = config.sessions.oidcUrl, redirectUris: List<Url> = listOf(nativeRedirectUri)): OIDCServerSettings? {
        return oidcRegistry.getByOrigin(origin) ?: run {
            val oidcConfig = oidcClient.getConfiguration(origin) ?: return@run null
            logger.trace { "providerLookup oidcConfig: $oidcConfig" }
            val credentials = oidcClient.createApplication(oidcConfig, redirectUris)

            OIDCServerSettings(
                origin = origin,
                authorizeUrl = oidcConfig.authorizationEndpoint,
                accessTokenUrl = oidcConfig.tokenEndpoint,
                credentials = credentials,
            ).also {
                logger.trace { "providerLookup persisting oidcConfig, $it" }
                oidcRegistry.setByOrigin(origin, it)
            }
        }
    }
}
