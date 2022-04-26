package io.ontola.studio

import io.ktor.http.ContentType
import io.ktor.http.DEFAULT_PORT
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.host
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.document.PageConfiguration
import io.ontola.cache.document.PageRenderContext
import io.ontola.cache.document.pageRenderContextFromCall
import io.ontola.cache.plugins.StudioConfig
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.persistentStorage
import io.ontola.cache.util.measured
import io.ontola.util.filename
import io.ontola.util.fullUrl
import io.ontola.util.origin
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import mu.KotlinLogging

private fun Int.onlyNonDefaultPort(): Int {
    if (this == 80 || this == 443) {
        return DEFAULT_PORT
    }

    return this
}

private val logger = KotlinLogging.logger {}

val StudioDeploymentKey = AttributeKey<PageRenderContext>("StudioDeploymentKey")

class Studio(private val configuration: Configuration) {
    class Configuration {
        lateinit var studioConfig: StudioConfig
        lateinit var distributionRepo: DistributionRepo
        lateinit var publicationRepo: PublicationRepo
        lateinit var pageConfig: PageConfiguration
    }

    private fun hostStudio(context: PipelineContext<Unit, ApplicationCall>) {
        val uri = context.call.fullUrl()

        val ctx = context.call.pageRenderContextFromCall(
            data = null,
            manifest = Manifest.forWebsite(Url(uri.origin())).copy(
                name = "Studio",
            ),
            uri = uri,
        )
        context.call.attributes.put(StudioDeploymentKey, ctx)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        lateinit var uri: Url
        val publication = context.call.measured("studioLookup") {
            val origin = context.call.request.origin
            uri = URLBuilder(
                protocol = URLProtocol.createOrDefault(origin.scheme),
                host = origin.host,
                port = origin.port.onlyNonDefaultPort(),
                pathSegments = context.call.request.path().split("/"),
            ).apply {
                parameters.appendAll(context.call.request.queryParameters)
            }.build()

            configuration.publicationRepo.match(uri)
        }

        publication ?: return context.proceed()
        logger.debug { "Prefix match for project ${publication.projectId} and distribution ${publication.distributionId}" }

        val distribution = configuration.distributionRepo.get(publication.projectId, publication.distributionId)
            ?: return context.call.respond(HttpStatusCode.NotFound)

        if (uri.filename() == "manifest.json") {
            context.call.respondOutputStream(ContentType.Application.Json) {
                context.application.cacheConfig.serializer.encodeToStream(distribution.manifest, this)
            }

            return context.finish()
        } else if (uri.filename() == "sitemap.txt") {
            context.call.respondText(distribution.sitemap)
            return context.finish()
        }

        if (distribution.data.isEmpty()) {
            context.call.respond(HttpStatusCode.NotFound)
            return context.finish()
        }

        val ctx = context.call.pageRenderContextFromCall(
            data = distribution.data,
            manifest = distribution.manifest,
            uri = uri,
        )

        context.call.attributes.put(StudioDeploymentKey, ctx)
    }

    companion object Plugin : ApplicationPlugin<ApplicationCallPipeline, Configuration, Studio> {
        override val key = AttributeKey<Studio>("Studio")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Studio {
            val configuration = Configuration().apply {
                studioConfig = pipeline.cacheConfig.studio
                distributionRepo = DistributionRepo(pipeline.persistentStorage)
                publicationRepo = PublicationRepo(pipeline.persistentStorage)
            }.apply(configure)
            val feature = Studio(configuration)

            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                if (call.request.host() == configuration.studioConfig.domain) {
                    feature.hostStudio(this)
                } else {
                    feature.intercept(this)
                }
            }

            return feature
        }
    }
}
