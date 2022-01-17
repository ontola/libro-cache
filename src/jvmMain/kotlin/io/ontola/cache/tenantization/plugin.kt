@file:UseSerializers(UrlSerializer::class)
package io.ontola.cache.tenantization

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.application.plugin
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.BadGatewayException
import io.ontola.cache.TenantNotFoundException
import io.ontola.cache.document.PageRenderContext
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.storage
import io.ontola.cache.studio.StudioDeploymentKey
import io.ontola.cache.util.UrlSerializer
import io.ontola.cache.util.measuredHit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import kotlin.properties.Delegates
import kotlin.time.ExperimentalTime

@Serializable
data class TenantFinderRequest(
    @SerialName("iri")
    val iri: String
)

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

class Tenantization(private val configuration: Configuration) {
    private val logger = KotlinLogging.logger {}

    class Configuration {
        val logger = KotlinLogging.logger {}

        /**
         * List of IRI prefixes which aren't subject to tenantization.
         */
        var blacklist: List<String> = emptyList()
        var dataExtensions: List<String> = emptyList()
        var tenantExpiration by Delegates.notNull<Long>()

        fun isBlacklisted(path: String): Boolean {
            return blacklist.any { fragment -> path.startsWith(fragment) } ||
                dataExtensions.any { path.endsWith(it) }
        }
    }

    @Throws(TenantNotFoundException::class)
    private suspend fun PipelineContext<*, ApplicationCall>.getWebsiteBase(): Url {
        val websiteIRI = call.request.closeToWebsiteIRI(configuration.logger)

        val websiteBase = cachedLookup(CachedLookupKeys.WebsiteBase, expiration = configuration.tenantExpiration) {
            getTenant(websiteIRI).websiteBase
        }(websiteIRI) ?: throw TenantNotFoundException()

        return Url(websiteBase)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun getManifest(context: PipelineContext<*, ApplicationCall>, websiteBase: Url): Manifest {
        val manifest = context.cachedLookup(
            CachedLookupKeys.Manifest,
            expiration = configuration.tenantExpiration
        ) {
            context.getManifest(Url(it))
        }(websiteBase.toString())

        return context.application.cacheConfig.serializer.decodeFromString(manifest!!)
    }

    private fun interceptDeployment(context: PipelineContext<Unit, ApplicationCall>, deployment: PageRenderContext) {
        try {
            val websiteBase = deployment.manifest.ontola.websiteIRI

            val baseOrigin = URLBuilder(websiteBase).apply { encodedPathSegments = emptyList() }.build()
            val currentIRI = Url("$baseOrigin${context.call.request.path()}")
            val manifest = deployment.manifest

            context.call.attributes.put(
                TenantizationKey,
                TenantData(
                    client = context.application.cacheConfig.client,
                    isBlackListed = false,
                    websiteIRI = manifest.ontola.websiteIRI,
                    websiteOrigin = baseOrigin,
                    currentIRI = currentIRI,
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

    private suspend fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        try {
            val websiteBase = context.getWebsiteBase()

            val baseOrigin = URLBuilder(websiteBase).apply { encodedPathSegments = emptyList() }.build()
            val currentIRI = Url("$baseOrigin${context.call.request.path()}")
            val manifest = getManifest(context, websiteBase)

            context.call.attributes.put(
                TenantizationKey,
                TenantData(
                    client = context.application.cacheConfig.client,
                    isBlackListed = false,
                    websiteIRI = manifest.ontola.websiteIRI,
                    websiteOrigin = baseOrigin,
                    currentIRI = currentIRI,
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

    companion object Plugin : ApplicationPlugin<ApplicationCallPipeline, Configuration, Tenantization> {
        override val key = AttributeKey<Tenantization>("Tenantization")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Tenantization {
            val configuration = Configuration().apply {
                tenantExpiration = pipeline.cacheConfig.tenantExpiration
            }.apply(configure)
            val feature = Tenantization(configuration)

            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                val path = call.request.path()
                if (call.attributes.getOrNull(StudioDeploymentKey) != null) {
                    val deployment = call.attributes[StudioDeploymentKey]
                    call.attributes.put(BlacklistedKey, false)
                    feature.interceptDeployment(this, deployment)
                } else if (configuration.isBlacklisted(path)) {
                    call.attributes.put(BlacklistedKey, true)
                } else {
                    call.attributes.put(BlacklistedKey, false)
                    feature.intercept(this)
                }
            }

            return feature
        }
    }
}

enum class CachedLookupKeys {
    Manifest,
    WebsiteBase,
}

@OptIn(ExperimentalTime::class)
private fun PipelineContext<*, ApplicationCall>.cachedLookup(
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
