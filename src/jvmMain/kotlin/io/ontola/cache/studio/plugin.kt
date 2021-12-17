package io.ontola.cache.studio

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.DEFAULT_PORT
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondOutputStream
import io.ktor.response.respondText
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.document.PageConfiguration
import io.ontola.cache.document.indexPage
import io.ontola.cache.document.pageRenderContextFromCall
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.persistentStorage
import io.ontola.cache.util.filename
import io.ontola.cache.util.measured
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToStream

private fun Int.onlyNonDefaultPort(): Int {
    if (this == 80 || this == 443) {
        return DEFAULT_PORT
    }

    return this
}

class Studio(private val configuration: Configuration) {
    class Configuration {
        lateinit var documentRepo: DocumentRepo
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
                encodedPath = context.call.request.path(),
            ).build().copy(parameters = context.call.request.queryParameters)

            configuration.documentRepo.documentKeyForRoute(uri)
        }

        docKey ?: return context.proceed()


        if (uri.filename() == "manifest.json") {
            val manifest = configuration.documentRepo.getManifest(docKey)

            if (manifest != null) {
                context.call.respondOutputStream(ContentType.Application.Json) {
                    context.application.cacheConfig.serializer.encodeToStream(manifest, this)
                }
            } else {
                context.call.respond(HttpStatusCode.NotFound)
            }

            return context.finish()
        } else if (uri.filename() == "sitemap.txt") {
            val sitemap = configuration.documentRepo.getSitemap(docKey)

            if (sitemap != null) {
                context.call.respondText(sitemap)
            } else {
                context.call.respond(HttpStatusCode.NotFound)
            }

            return context.finish()
        }

        val source = configuration.documentRepo.getSource(docKey)
        val manifest = configuration.documentRepo.getManifest(docKey) ?: error("Document without manifest")

        if (source == null) {
            context.call.respond(HttpStatusCode.NotFound)
            return context.finish()
        }

        val data = context.measured("source to hextuples") {
            sourceToHextuples(source, uri)
        }

        val seed = context.measured("hex to json string") {
            val serializer = context.application.cacheConfig.serializer
            data.joinToString("\n") { serializer.encodeToString(it) }
        }

        val ctx = context.call.pageRenderContextFromCall(
            seed = seed,
            manifest = manifest,
            uri = uri,
        )

        context.measured("respondHtml") {
            context.call.respondHtml(HttpStatusCode.OK) {
                indexPage(ctx)
            }
        }

        context.finish()
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, Studio> {
        override val key = AttributeKey<Studio>("Studio")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Studio {
            val configuration = Configuration().apply {
                documentRepo = DocumentRepo(pipeline.persistentStorage)
            }.apply(configure)
            val feature = Studio(configuration)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                feature.intercept(this)
            }

            return feature
        }
    }
}