package io.ontola.cache.health

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.application
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.cacheConfig

class EnvironmentCheck : Check() {
    init {
        name = "Environment variables"
    }

    override suspend fun runTest(context: PipelineContext<Unit, ApplicationCall>): Exception? {
        val config = context.application.cacheConfig
        val env = config.env
        val failed = buildList {
            fun checkValue(k: String, v: String?) {
                if (v.isNullOrBlank()) {
                    add(k)
                }
            }

            if (env != "development") {
                checkValue("invalidationChannel", config.redis.invalidationChannel)
                checkValue("reportingKey", config.serverReportingKey)
                checkValue("mapboxKey", config.maps?.key)
                checkValue("mapboxUsername", config.maps?.username)
            }

            checkValue("redisUrl", config.redis.uri.toString())
            checkValue("clientId", config.sessions.clientId)
            checkValue("clientSecret", config.sessions.clientSecret)
            checkValue("jwtEncryptionToken", config.sessions.jwtEncryptionToken)
            checkValue("oAuthToken", config.sessions.oAuthToken)
            checkValue("sessionSecret", config.sessions.sessionSecret)
        }

        if (failed.isNotEmpty()) {
            throw Exception("Invalid: ${failed.joinToString()}")
        }

        return null
    }
}
