package tools.empathy.libro.server.plugins

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.server.request.path
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.util.AttributeKey
import java.util.UUID

class DeviceIdConfiguration {
    var blacklist: List<String> = emptyList()
}

val DeviceId = createApplicationPlugin(name = "DeviceId", ::DeviceIdConfiguration) {

    fun isBlacklisted(path: String): Boolean {
        return pluginConfig.blacklist.any { fragment -> path.startsWith(fragment) }
    }

    fun generateSessionId(call: ApplicationCall): String {
        val id = UUID.randomUUID().toString()
        call.sessions.set(id)

        return id
    }

    onCall { call ->
        if (!isBlacklisted(call.request.path())) {
            val deviceId = call.sessions.get<String>() ?: generateSessionId(call)
            call.attributes.put(DeviceIdKey, deviceId)
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
