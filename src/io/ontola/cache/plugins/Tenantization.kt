@file:UseSerializers(UrlSerializer::class)

package io.ontola.cache.plugins

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.ResponseException
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.http.takeFrom
import io.ktor.request.header
import io.ktor.request.path
import io.ktor.util.AttributeKey
import io.ktor.util.InternalAPI
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.BadGatewayException
import io.ontola.cache.TenantNotFoundException
import io.ontola.cache.util.UrlSerializer
import io.ontola.cache.util.configureClientLogging
import io.ontola.cache.util.copy
import io.ontola.cache.util.measuredHit
import io.ontola.cache.util.origin
import io.ontola.cache.util.proxySafeHeaders
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KLogger
import mu.KotlinLogging
import kotlin.properties.Delegates
import kotlin.time.ExperimentalTime

fun ensureTrailingSlash(value: String) = if (value.endsWith('/')) value else "$value/"

@Serializable
data class OntolaManifest(
    @SerialName("allowed_external_sources")
    val allowedExternalSources: Set<String>? = null,
    @SerialName("css_class")
    val cssClass: String = "default",
    @SerialName("header_background")
    val headerBackground: String = "primary",
    @SerialName("header_text")
    val headerText: String = "auto",
    @SerialName("matomo_hostname")
    val matomoHostname: String? = null,
    val preconnect: Set<String>? = null,
    val preload: Set<String> = emptySet(),
    @SerialName("matomo_site_id")
    val matomoSiteId: String? = null,
    @SerialName("primary_color")
    val primaryColor: String = "#475668",
    @SerialName("secondary_color")
    val secondaryColor: String = "#d96833",
    @SerialName("styled_headers")
    val styledHeaders: String? = null,
    val theme: String? = null,
    @SerialName("theme_options")
    val themeOptions: String? = null,
    val tracking: List<Tracking> = emptyList(),
    @SerialName("website_iri")
    val websiteIRI: Url,
    @SerialName("websocket_path")
    val websocketPath: String? = null,
)

@Serializable
data class ServiceWorker(
    val src: String = "/",
    val scope: String = if (src == "/") "/sw.js" else "$src/sw.js",
)

@Serializable
data class Icon(
    val src: String,
    val sizes: String,
    val type: String,
)

@Serializable
enum class TrackerType {
    GUA,
    GTM,
    PiwikPro,
    Matomo,
}

@Serializable
data class Tracking(
    val host: String? = null,
    val type: TrackerType,
    @SerialName("container_id")
    val containerId: String,
)

@Serializable
data class Manifest(
    @SerialName("rdf_type")
    val rdfType: String? = null,
    @SerialName("canonical_iri")
    val canonicalIri: String? = null,
    @SerialName("background_color")
    val backgroundColor: String = "#eef0f2",
    val dir: String = "rtl",
    val display: String = "standalone",
    val icons: List<Icon>? = null,
    val lang: String = "en-US",
    val name: String = "Libro",
    val ontola: OntolaManifest,
    val serviceworker: ServiceWorker = ServiceWorker(),
    @SerialName("short_name")
    val shortName: String = name,
    @SerialName("start_url")
    val startUrl: String = ensureTrailingSlash(serviceworker.scope),
    @SerialName("scope")
    val scope: String,
    @SerialName("theme_color")
    val themeColor: String = "#475668",
    @SerialName("created_at")
    val createdAt: String? = null,
) {
    companion object {
        fun forWebsite(websiteIRI: Url): Manifest = Manifest(
            ontola = OntolaManifest(
                websiteIRI = websiteIRI,
            ),
            scope = websiteIRI.encodedPath,
            serviceworker = ServiceWorker(
                scope = websiteIRI.encodedPath,
            ),
//            startUrl = Url("$websiteIRI/")
        )
    }
}

@Serializable
data class TenantFinderRequest(
    @SerialName("iri")
    val iri: String
)

@Serializable
data class TenantFinderResponse(
    val uuid: String? = null,
    @SerialName("all_shortnames")
    val allShortnames: List<String> = emptyList(),
    /**
     * The host and possible path of the website.
     */
    @SerialName("iri_prefix")
    val iriPrefix: String,
    @SerialName("header_background")
    val headerBackground: String? = null,
    @SerialName("header_text")
    val headerText: String? = null,
    @SerialName("secondary_color")
    val secondaryColor: String? = "#d96833",
    @SerialName("primary_color")
    val primaryColor: String? = "#475668",
    @SerialName("database_schema")
    val databaseSchema: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
) {
    @Transient
    lateinit var websiteBase: String
}

data class TenantData(
    internal val client: HttpClient,
    val isBlackListed: Boolean,
    val websiteIRI: Url,
    val websiteOrigin: Url,
    val currentIRI: Url,
    val manifest: Manifest,
)

private val TenantizationKey = AttributeKey<TenantData>("TenantizationKey")
private val BlacklistedKey = AttributeKey<Boolean>("BlacklistedKey")

internal val ApplicationCall.tenant: TenantData
    get() = attributes.getOrNull(TenantizationKey) ?: reportMissingTenantization()

internal val ApplicationCall.blacklisted: Boolean
    get() = attributes.getOrNull(BlacklistedKey) ?: reportMissingTenantization()

private fun ApplicationCall.reportMissingTenantization(): Nothing {
    application.feature(Tenantization) // ensure the feature is installed
    throw TenantizationNotYetConfiguredException()
}

private fun Boolean.toInt(): Int = if (this) 1 else 0

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
        val serializer = Json {
            isLenient = false
            ignoreUnknownKeys = false
        }
        var client: HttpClient = HttpClient(CIO) {
            followRedirects = false
            install(Logging) {
                configureClientLogging()
            }
            install(JsonFeature) {
                serializer = KotlinxSerializer(this@Configuration.serializer)
            }
        }
        var tenantExpiration by Delegates.notNull<Long>()

        fun isBlacklisted(path: String): Boolean {
            return blacklist.any { fragment -> path.startsWith(fragment) } ||
                dataExtensions.any { path.endsWith(it) }
        }
    }

    @Throws(TenantNotFoundException::class)
    private suspend fun PipelineContext<*, ApplicationCall>.getWebsiteBase(): Url {
        val websiteIRI = closeToWebsiteIRI(call.request.path(), call.request.headers, configuration.logger)

        val websiteBase = cachedLookup(CachedLookupKeys.WebsiteBase, expiration = configuration.tenantExpiration) {
            getTenant(websiteIRI, configuration).websiteBase
        }(websiteIRI) ?: throw TenantNotFoundException()

        return Url(websiteBase)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun PipelineContext<*, ApplicationCall>.getManifest(websiteBase: Url, services: Services): Manifest {
        val manifest = cachedLookup(CachedLookupKeys.Manifest, expiration = configuration.tenantExpiration) {
            val manifestRequest = configuration.client.get<HttpResponse>(services.route("${Url(it).fullPath}/manifest.json")) {
                headers {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    header("Website-IRI", it)

                    proxySafeHeaders(context.request)
                    copy(HttpHeaders.XForwardedFor, context.request)
                    copy("X-Real-Ip", context.request)
                }
            }

            if (manifestRequest.status != HttpStatusCode.OK) {
                throw ResponseException(manifestRequest, manifestRequest.receive())
            }

            manifestRequest.receive()
        }(websiteBase.toString())

        return configuration.serializer.decodeFromString(manifest!!)
    }

    private suspend fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        try {
            val websiteBase = context.getWebsiteBase()

            val baseOrigin = websiteBase.copy(encodedPath = "")
            val currentIRI = Url("$baseOrigin${context.call.request.path()}")
            val manifest = context.getManifest(websiteBase, context.call.services)

            context.call.attributes.put(
                TenantizationKey,
                TenantData(
                    client = configuration.client,
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

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, Tenantization> {
        override val key = AttributeKey<Tenantization>("Tenantization")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Tenantization {
            val configuration = Configuration().apply {
                client = pipeline.cacheConfig.client
                tenantExpiration = pipeline.cacheConfig.tenantExpiration
            }.apply(configure)
            val feature = Tenantization(configuration)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                val path = this.call.request.path()
                if (configuration.isBlacklisted(path)) {
                    this.call.attributes.put(BlacklistedKey, true)
                } else {
                    this.call.attributes.put(BlacklistedKey, false)
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
            {
                application.storage.getString(prefix.name, dependency)
            },
            {
                block(dependency)?.also {
                    application.storage.setString(prefix.name, dependency, value = it, expiration = expiration)
                }
            }
        )
    }
}

@OptIn(InternalAPI::class)
private suspend fun PipelineContext<*, ApplicationCall>.getTenant(
    resourceIri: String,
    configuration: Tenantization.Configuration,
): TenantFinderResponse {
    return configuration.client.get<TenantFinderResponse> {
        url.apply {
            takeFrom(call.services.route("/_public/spi/find_tenant"))
            parameters["iri"] = resourceIri
        }
        headers {
            header("Accept", ContentType.Application.Json)
            copy("X-Request-Id", context.request)
        }
    }.apply {
        val proto = context.request.header("X-Forwarded-Proto")?.split(',')?.firstOrNull() ?: context.request.header("scheme") ?: "http"
        websiteBase = "$proto://$iriPrefix"
    }
}

internal fun closeToWebsiteIRI(requestPath: String, headers: Headers, logger: KLogger): String {
    val path = requestPath.removeSuffix("link-lib/bulk")
    val authority = listOf("X-Forwarded-Host", "origin", "host", "authority")
        .find { header -> headers[header] != null }
        ?.let { header -> headers[header]!! }
        ?: throw Exception("No header usable for authority present")

//        if (authority.contains(':')) {
//            return "$authority$path"
//        }

    val proto = headers["X-Forwarded-Proto"]?.split(',')?.firstOrNull()
        ?: headers["origin"]?.split(":")?.firstOrNull()
        ?: throw Exception("No Forwarded host nor authority scheme")

    val authoritativeOrigin = if (authority.contains(':')) {
        authority
    } else {
        "$proto://$authority"
    }

    return headers["Website-IRI"]
        ?.let { websiteIRI ->
            if (Url(websiteIRI).origin() != authoritativeOrigin) {
                logger.warn("Website-Iri does not correspond with authority headers (website-iri: '$websiteIRI', authority: '$authoritativeOrigin')")
            }
            websiteIRI
        } ?: "$authoritativeOrigin$path".let { it.dropLast(it.endsWith('/').toInt()) }
}
