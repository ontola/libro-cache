@file:UseSerializers(UrlSerializer::class)

package tools.empathy.libro.server.tenantization

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.server.request.uri
import io.ktor.util.AttributeKey
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import tools.empathy.libro.server.BadGatewayException
import tools.empathy.libro.server.TenantNotFoundException
import tools.empathy.libro.server.configuration.LibroConfig
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.document.PageRenderContext
import tools.empathy.libro.server.plugins.LookupKeys
import tools.empathy.libro.server.plugins.blacklisted
import tools.empathy.libro.server.plugins.persistentStorage
import tools.empathy.libro.server.plugins.setManifestLanguage
import tools.empathy.libro.server.util.UrlSerializer
import tools.empathy.libro.server.util.origin
import tools.empathy.libro.webmanifest.Manifest
import tools.empathy.studio.StudioDeploymentKey
import tools.empathy.url.fullUrl
import tools.empathy.url.origin
import tools.empathy.url.rebase

private val TenantizationKey = AttributeKey<TenantData>("TenantizationKey")

internal val ApplicationCall.tenant: TenantData
    get() = attributes.getOrNull(TenantizationKey) ?: reportMissingTenantization()

internal val ApplicationCall.tenantOrNull: TenantData?
    get() = attributes.getOrNull(TenantizationKey)

private fun ApplicationCall.reportMissingTenantization(): Nothing {
    application.plugin(Tenantization) // ensure the feature is installed
    throw TenantizationNotYetConfiguredException()
}

class TenantizationNotYetConfiguredException :
    IllegalStateException("Libro tenantization are not yet ready: you are asking it to early before the Tenantization feature.")

class TenantizationConfiguration {
    val logger = KotlinLogging.logger {}

    var staticTenants: Map<String, TenantData> = emptyMap()

    fun staticTenant(url: Url): TenantData? = staticTenants[url.host]
}

val Tenantization = createApplicationPlugin(name = "Tenantization", ::TenantizationConfiguration) {
    val logger = KotlinLogging.logger {}

    @Throws(TenantNotFoundException::class)
    suspend fun getManifest(call: ApplicationCall, websiteBase: String): Manifest {
        val manifest = application.persistentStorage.getHashValue(LookupKeys.Manifest.name, hashKey = websiteBase) ?: throw TenantNotFoundException()

        return call.application.libroConfig.serializer.decodeFromString(manifest)
    }

    fun interceptDeployment(call: ApplicationCall, libroConfig: LibroConfig, deployment: PageRenderContext) {
        val websiteBase = deployment.manifest.ontola.websiteIRI

        val baseOrigin = URLBuilder(websiteBase).apply { encodedPathSegments = emptyList() }.build()
        val manifest = deployment.manifest

        call.attributes.put(
            TenantizationKey,
            TenantData(
                client = libroConfig.client,
                websiteIRI = manifest.ontola.websiteIRI,
                websiteOrigin = baseOrigin,
                manifest = manifest,
            )
        )
    }

    suspend fun intercept(call: ApplicationCall, libroConfig: LibroConfig) {
        try {
            val websiteBase = call.getWebsiteBase()

            val baseOrigin = URLBuilder(Url(websiteBase)).apply { encodedPathSegments = emptyList() }.build()
            val manifest = getManifest(call, websiteBase)

            call.setManifestLanguage(manifest.lang)
            call.attributes.put(
                TenantizationKey,
                TenantData(
                    client = libroConfig.client,
                    websiteIRI = manifest.ontola.websiteIRI,
                    websiteOrigin = baseOrigin,
                    manifest = manifest,
                )
            )
        } catch (e: ResponseException) {
            val origin = call.fullUrl().origin().let { Url(it) }

            call.attributes.put(
                TenantizationKey,
                TenantData(
                    client = libroConfig.client,
                    websiteIRI = origin,
                    websiteOrigin = origin,
                    manifest = Manifest.forWebsite(origin),
                )
            )

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
        if (call.attributes.getOrNull(StudioDeploymentKey) != null) {
            val deployment = call.attributes[StudioDeploymentKey]
            interceptDeployment(call, this@createApplicationPlugin.application.libroConfig, deployment)
        } else if (!call.blacklisted) {
            val url = Url(call.request.origin()).rebase(call.request.uri)
            val staticTenant = pluginConfig.staticTenant(url)
            if (staticTenant != null) {
                if (staticTenant.allowUnsafe && Url(call.request.uri).protocol == URLProtocol.HTTP) {
                    val unsafeIRI = staticTenant.unsafeIRI

                    call.attributes.put(
                        TenantizationKey,
                        staticTenant.copy(
                            websiteIRI = unsafeIRI,
                            websiteOrigin = Url(unsafeIRI.origin()),
                            manifest = staticTenant.manifest.copy(
                                ontola = staticTenant.manifest.ontola.copy(
                                    websiteIRI = unsafeIRI,
                                )
                            )
                        )
                    )
                } else {
                    call.attributes.put(TenantizationKey, staticTenant)
                }
            } else {
                intercept(call, this@createApplicationPlugin.application.libroConfig)
            }
        }
    }
}
