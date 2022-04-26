package io.ontola.cache.health

import io.ktor.server.application.ApplicationCall
import io.ontola.cache.plugins.storage

class RedisCheck : Check() {
    init {
        name = "Redis connectivity"
    }

    override suspend fun runTest(call: ApplicationCall): Exception? {
        call.application.storage.getString()
        return null
    }
}
