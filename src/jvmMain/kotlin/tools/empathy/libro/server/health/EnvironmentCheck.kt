package tools.empathy.libro.server.health

import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.runBlocking
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.module
import tools.empathy.libro.server.plugins.storage
import tools.empathy.libro.server.sessions.oidc.OIDCSettingsManager

class EnvironmentCheck : Check() {
    init {
        name = "Environment variables"
    }

    override suspend fun runTest(call: ApplicationCall): Exception? {
        val config = call.application.libroConfig
        val env = config.env
        val failed = buildList {
            fun checkValue(k: String, v: String?) {
                if (v.isNullOrBlank()) {
                    add(k)
                }
            }

            val oidcSettings = runBlocking { OIDCSettingsManager(config, call.application.storage).get() }

            if (env != "development") {
                checkValue("invalidationChannel", config.redis.invalidationChannel)
                checkValue("reportingKey", config.serverReportingKey)
                checkValue("mapboxKey", config.maps?.key)
                checkValue("mapboxUsername", config.maps?.username)
            }

            checkValue("redisUrl", config.redis.uri.toString())
            checkValue("clientName", config.sessions.clientName)
            checkValue("clientId", oidcSettings?.credentials?.clientId)
            checkValue("clientSecret", oidcSettings?.credentials?.clientSecret)
            checkValue("jwtEncryptionToken", config.sessions.jwtEncryptionToken)
            checkValue("sessionSecret", config.sessions.sessionSecret)
        }

        if (failed.isNotEmpty()) {
            throw Exception("Invalid: ${failed.joinToString()}")
        }

        return null
    }
}
