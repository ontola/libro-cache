package io.ontola.cache.studio

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.features.origin
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.document.PageConfiguration
import io.ontola.cache.document.indexPage
import io.ontola.cache.document.pageRenderContextFromCall
import io.ontola.cache.plugins.CacheSession
import io.ontola.cache.plugins.storage
import io.ontola.cache.util.filename

class Studio(private val configuration: Configuration) {
    class Configuration {
        lateinit var documentRepo: DocumentRepo
        lateinit var pageConfig: PageConfiguration
    }

    private suspend fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        val origin = context.call.request.origin
        val uri = URLBuilder(
            protocol = URLProtocol.createOrDefault(origin.scheme),
            host = origin.host,
            port = origin.port,
            encodedPath = context.call.request.path(),
        ).build().copy(parameters = context.call.request.queryParameters)

        val docKey = configuration.documentRepo.documentKeyForRoute(uri) ?: return context.proceed()

        if (uri.filename() == "manifest.json") {
            val manifest = configuration.documentRepo.getManifest(docKey)

            if (manifest != null) {
                context.call.respond(manifest)
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

        val ctx = context.call.pageRenderContextFromCall().apply {
            seed = source
        }

        context.call.respondHtml(HttpStatusCode.OK) {
            indexPage(ctx)
        }

        context.finish()
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, Studio> {
        override val key = AttributeKey<Studio>("Studio")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Studio {
            val configuration = Configuration().apply {
                documentRepo = DocumentRepo(pipeline.storage)
            }.apply(configure)
            val feature = Studio(configuration)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                feature.intercept(this)
            }

            return feature
        }
    }
}

private val StudioKey = AttributeKey<String>("StudioKey")

internal val ApplicationCall.studio: String
    get() = attributes.getOrNull(StudioKey) ?: reportMissingNonce()

private fun ApplicationCall.reportMissingNonce(): Nothing {
    application.feature(CacheSession) // ensure the feature is installed
    throw NonceNotYetConfiguredException()
}

class NonceNotYetConfiguredException :
    IllegalStateException("Studio is not yet ready: you are asking it to early before the Studio feature.")
