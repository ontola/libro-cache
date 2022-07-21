package tools.empathy.libro.server.health

import io.ktor.server.application.ApplicationCall
import tools.empathy.libro.server.plugins.storage

class RedisCheck : Check() {
    init {
        name = "Redis connectivity"
    }

    override suspend fun runTest(call: ApplicationCall): Exception? {
        call.application.storage.getString()
        return null
    }
}
