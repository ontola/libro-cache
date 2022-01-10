package io.ontola.cache.plugins

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.path
import io.ktor.server.response.respondRedirect
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext

private const val redirectPrefix = "redirect"

fun removeTrailingSlash(value: String) = if (value.endsWith('/')) value.dropLast(1) else value

class Redirect(
    private val storage: Storage,
) {
    class Configuration {
        lateinit var storage: Storage
    }

    private suspend fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        val uri = removeTrailingSlash("${context.call.request.origin.host}${context.call.request.path()}")
        val redirectLocation = storage.getString(redirectPrefix, uri)

        redirectLocation ?: return context.proceed()

        context.call.respondRedirect(redirectLocation, true)
        context.finish()
    }

    companion object Plugin : ApplicationPlugin<ApplicationCallPipeline, Configuration, Redirect> {
        override val key = AttributeKey<Redirect>("Redirect")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Redirect {
            val config = Configuration()
                .apply {
                    storage = pipeline.persistentStorage
                }.apply(configure)
            val feature = Redirect(
                config.storage,
            )

            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                feature.intercept(this)
            }

            return feature
        }
    }
}
