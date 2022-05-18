@file:UseSerializers(UrlSerializer::class)
package io.ontola.cache.tenantization

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.server.locations.url
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.util.AttributeKey
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.BadGatewayException
import io.ontola.cache.TenantNotFoundException
import io.ontola.cache.document.PageRenderContext
import io.ontola.cache.plugins.CacheConfig
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.setManifestLanguage
import io.ontola.cache.plugins.storage
import io.ontola.cache.util.UrlSerializer
import io.ontola.cache.util.measuredHit
import io.ontola.studio.StudioDeploymentKey
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import kotlin.properties.Delegates
import kotlin.time.ExperimentalTime

private val TenantizationKey = AttributeKey<TenantData>("TenantizationKey")
private val BlacklistedKey = AttributeKey<Boolean>("BlacklistedKey")

internal val ApplicationCall.tenant: TenantData
    get() = attributes.getOrNull(TenantizationKey) ?: reportMissingTenantization()

internal val ApplicationCall.tenantOrNull: TenantData?
    get() = attributes.getOrNull(TenantizationKey)

internal val ApplicationCall.blacklisted: Boolean
    get() = attributes.getOrNull(BlacklistedKey) ?: reportMissingTenantization()

private fun ApplicationCall.reportMissingTenantization(): Nothing {
    application.plugin(Tenantization) // ensure the feature is installed
    throw TenantizationNotYetConfiguredException()
}

class TenantizationNotYetConfiguredException :
    IllegalStateException("Libro tenantization are not yet ready: you are asking it to early before the Tenantization feature.")

class TenantizationConfiguration {
    val logger = KotlinLogging.logger {}

    /**
     * List of IRI prefixes which aren't subject to tenantization.
     */
    var blacklist: List<String> = emptyList()
    var dataExtensions: List<String> = emptyList()
    var tenantExpiration by Delegates.notNull<Long>()
    var staticTenants: Map<String, TenantData> = emptyMap()

    fun staticTenant(url: Url): TenantData? = staticTenants[url.host]

    fun isBlacklisted(path: String): Boolean {
        return blacklist.any { fragment -> path.startsWith(fragment) } ||
            dataExtensions.any { path.endsWith(it) }
    }

    fun complete(cacheConfig: CacheConfig) {
        this.tenantExpiration = cacheConfig.tenantExpiration
    }
}

val Tenantization = createApplicationPlugin(name = "Tenantization", ::TenantizationConfiguration) {
    val logger = KotlinLogging.logger {}
    pluginConfig.complete(application.cacheConfig)

    @Throws(TenantNotFoundException::class)
    suspend fun ApplicationCall.getWebsiteBase(): Url {
        val websiteIRI = request.closeToWebsiteIRI(pluginConfig.logger)

        val websiteBase = cachedLookup(CachedLookupKeys.WebsiteBase, expiration = pluginConfig.tenantExpiration) {
            getTenant(websiteIRI).websiteBase
        }(websiteIRI) ?: throw TenantNotFoundException()

        return Url(websiteBase)
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getManifest(call: ApplicationCall, websiteBase: Url): Manifest {
        val manifest = call.cachedLookup(
            CachedLookupKeys.Manifest,
            expiration = pluginConfig.tenantExpiration
        ) {
            call.getManifest(Url(it))
        }(websiteBase.toString())

        return call.application.cacheConfig.serializer.decodeFromString(manifest!!)
    }

    fun interceptDeployment(call: ApplicationCall, cacheConfig: CacheConfig, deployment: PageRenderContext) {
        try {
            val websiteBase = deployment.manifest.ontola.websiteIRI

            val baseOrigin = URLBuilder(websiteBase).apply { encodedPathSegments = emptyList() }.build()
            val currentIRI = Url("$baseOrigin${call.request.path()}")
            val manifest = deployment.manifest

            call.attributes.put(
                TenantizationKey,
                TenantData(
                    client = cacheConfig.client,
                    isBlackListed = false,
                    websiteIRI = manifest.ontola.websiteIRI,
                    websiteOrigin = baseOrigin,
                    manifest = manifest,
                )
            )
        } catch (e: ResponseException) {
            when (val status = e.response.status) {
                HttpStatusCode.NotFound -> throw TenantNotFoundException()
                HttpStatusCode.BadGateway -> throw BadGatewayException()
                else -> {
                    logger.debug { "Unexpected status $status while getting tenant ($e)" }
                    throw e
                }
            }
        }
    }

    fun intercept(call: ApplicationCall, cacheConfig: CacheConfig) = runBlocking {
        try {
            val websiteBase = call.getWebsiteBase()

            val baseOrigin = URLBuilder(websiteBase).apply { encodedPathSegments = emptyList() }.build()
            val manifest = getManifest(call, websiteBase)

            call.setManifestLanguage(manifest.lang)
            call.attributes.put(
                TenantizationKey,
                TenantData(
                    client = cacheConfig.client,
                    isBlackListed = false,
                    websiteIRI = manifest.ontola.websiteIRI,
                    websiteOrigin = baseOrigin,
                    manifest = manifest,
                )
            )
        } catch (e: ResponseException) {
            when (val status = e.response.status) {
                HttpStatusCode.NotFound -> throw TenantNotFoundException()
                HttpStatusCode.BadGateway -> throw BadGatewayException()
                else -> {
                    logger.debug { "Unexpected status $status while getting tenant ($e)" }
                    throw e
                }
            }
        }
    }

    onCall { call ->
        val url = Url(call.url(call.request.uri))
        val path = call.request.path()

        if (call.attributes.getOrNull(StudioDeploymentKey) != null) {
            val deployment = call.attributes[StudioDeploymentKey]
            call.attributes.put(BlacklistedKey, false)
            interceptDeployment(call, this@createApplicationPlugin.application.cacheConfig, deployment)
        } else if (pluginConfig.isBlacklisted(path)) {
            call.attributes.put(BlacklistedKey, true)
        } else {
            call.attributes.put(BlacklistedKey, false)

            val staticTenant = pluginConfig.staticTenant(url)
            if (staticTenant != null)
                call.attributes.put(TenantizationKey, staticTenant)
            else
                intercept(call, this@createApplicationPlugin.application.cacheConfig)
        }
    }
}

enum class CachedLookupKeys {
    Manifest,
    WebsiteBase,
}

@OptIn(ExperimentalTime::class)
private fun ApplicationCall.cachedLookup(
    prefix: CachedLookupKeys,
    expiration: Long,
    block: suspend (v: String) -> String?,
): suspend (v: String) -> String? {
    if (expiration == 0L) {
        return block
    }

    return { dependency ->
        measuredHit(
            prefix.name,
            block = {
                application.storage.getString(prefix.name, dependency)
            },
            onMissed = {
                block(dependency)?.also {
                    application.storage.setString(prefix.name, dependency, value = it, expiration = expiration)
                }
            }
        )
    }
}
