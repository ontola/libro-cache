package tools.empathy.libro.server.sessions.oidc

import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import tools.empathy.libro.server.configuration.LibroConfig
import tools.empathy.libro.server.plugins.Storage

class OIDCSettingsManager(
    private val config: LibroConfig,
    storage: Storage,
) {
    private val defaultRedirects = listOf(nativeRedirectUri)
    private val logger = KotlinLogging.logger {}
    private val oidcRegistry = OIDCSettingsRepository(storage)
    private val oidcClient = OIDCClient(config.client, config.sessions.clientName)
    private val oidcServerSettings = mutableMapOf<Url, OIDCServerSettings>()

    suspend fun getOrCreate(
        origin: Url = config.sessions.oidcUrl,
        redirectUris: List<Url> = defaultRedirects,
    ): OIDCServerSettings? {
        return get(origin) ?: create(origin, redirectUris)
    }

    fun getOrCreateBlocking(
        origin: Url = config.sessions.oidcUrl,
        redirectUris: List<Url> = defaultRedirects,
    ): OIDCServerSettings? = runBlocking {
        getOrCreate(origin, redirectUris)
    }

    suspend fun get(origin: Url = config.sessions.oidcUrl): OIDCServerSettings? {
        return oidcServerSettings[origin] ?: oidcRegistry.getByOrigin(origin)
    }

    suspend fun refresh(origin: Url = config.sessions.oidcUrl) {
        val current = get(origin) ?: throw Exception("No OIDC settings found to refresh for $origin")
        delete(origin)
        getOrCreate(origin, current.credentials.redirectUris)
    }

    suspend fun delete(origin: Url = config.sessions.oidcUrl) {
        oidcServerSettings.remove(origin)
        oidcRegistry.deleteByOrigin(origin)
    }

    suspend fun create(origin: Url, redirectUris: List<Url> = defaultRedirects): OIDCServerSettings? {
        val oidcConfig = oidcClient.getConfiguration(origin) ?: return null

        val credentials = oidcClient.createApplication(oidcConfig, redirectUris)

        return OIDCServerSettings(
            origin = origin,
            authorizeUrl = oidcConfig.authorizationEndpoint,
            accessTokenUrl = oidcConfig.tokenEndpoint,
            credentials = credentials,
        ).also {
            oidcServerSettings[origin] = it
            logger.trace { "persisting oidc config: $it" }
            oidcRegistry.setByOrigin(origin, it)
        }
    }
}
