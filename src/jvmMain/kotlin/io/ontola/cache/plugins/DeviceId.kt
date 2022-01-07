package io.ontola.cache.plugins

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.application.plugin
import io.ktor.server.request.path
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import java.util.UUID

class DeviceId(private val configuration: Configuration) {
    class Configuration {
        var blacklist: List<String> = emptyList()
    }

    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        if (!isBlacklisted(context.call.request.path())) {
            val deviceId = context.call.sessions.get<String>() ?: generateSessionId(context)
            context.call.attributes.put(DeviceIdKey, deviceId)
        }
    }

    private fun isBlacklisted(path: String): Boolean {
        return configuration.blacklist.any { fragment -> path.startsWith(fragment) }
    }

    private fun generateSessionId(context: PipelineContext<Unit, ApplicationCall>): String {
        val id = UUID.randomUUID().toString()
        context.call.sessions.set(id)

        return id
    }

    companion object Plugin : ApplicationPlugin<ApplicationCallPipeline, Configuration, DeviceId> {
        override val key = AttributeKey<DeviceId>("DeviceId")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): DeviceId {
            val configuration = Configuration().apply(configure)
            val feature = DeviceId(configuration)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                feature.intercept(this)
            }

            return feature
        }
    }
}

private val DeviceIdKey = AttributeKey<String>("DeviceIdKey")

internal val ApplicationCall.deviceId: String
    get() = attributes.getOrNull(DeviceIdKey) ?: reportMissingDeviceId()

private fun ApplicationCall.reportMissingDeviceId(): Nothing {
    application.plugin(CacheSession) // ensure the feature is installed
    throw DeviceIdNotYetConfiguredException()
}

class DeviceIdNotYetConfiguredException :
    IllegalStateException("DeviceId is not yet ready: you are asking it to early before the DeviceId feature.")
