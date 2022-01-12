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
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.document.PageConfiguration
import io.ontola.cache.document.PageRenderContext
import io.ontola.cache.document.pageRenderContextFromCall
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.persistentStorage
import io.ontola.cache.util.measured
import io.ontola.rdf.hextuples.Hextuple
import io.ontola.util.filename
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream

private fun Int.onlyNonDefaultPort(): Int {
    if (this == 80 || this == 443) {
        return DEFAULT_PORT
    }

    return this
}

val StudioDeploymentKey = AttributeKey<PageRenderContext>("StudioDeploymentKey")

class Studio(private val configuration: Configuration) {
    class Configuration {
        lateinit var distributionRepo: DistributionRepo
        lateinit var pageConfig: PageConfiguration
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        lateinit var uri: Url
        val docKey = context.measured("studio lookup") {
            val origin = context.call.request.origin
            uri = URLBuilder(
                protocol = URLProtocol.createOrDefault(origin.scheme),
                host = origin.host,
                port = origin.port.onlyNonDefaultPort(),
                pathSegments = context.call.request.path().split("/"),
            ).apply {
                parameters.appendAll(context.call.request.queryParameters)
            }.build()

            configuration.distributionRepo.documentKeyForRoute(uri)
        }

        docKey ?: return context.proceed()
        val distribution = configuration.distributionRepo.get(docKey) ?: return context.call.respond(HttpStatusCode.NotFound)

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
            data = distribution.data.map { Hextuple.fromArray(it) },
            manifest = distribution.manifest,
            uri = uri,
        )

        context.call.attributes.put(StudioDeploymentKey, ctx)
    }

    companion object Plugin : ApplicationPlugin<ApplicationCallPipeline, Configuration, Studio> {
        override val key = AttributeKey<Studio>("Studio")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Studio {
            val configuration = Configuration().apply {
                distributionRepo = DistributionRepo(pipeline.persistentStorage)
            }.apply(configure)
            val feature = Studio(configuration)

            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                feature.intercept(this)
            }

            return feature
        }
    }
}
