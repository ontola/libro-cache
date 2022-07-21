package tools.empathy.libro.server.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.util.AttributeKey
import kotlinx.serialization.decodeFromString
import tools.empathy.libro.server.plugins.cacheConfig
import tools.empathy.libro.server.plugins.sessionManager
import tools.empathy.libro.server.sessions.SessionData

fun Routing.mountTestingRoutes() {
    post("/_testing/setSession") {
        if (call.attributes.contains(AttributeKey<Unit>("StatusPagesTriggered"))) {
            return@post
        }

        val newSession = call.application.cacheConfig.serializer.decodeFromString<SessionData>(call.receive<String>())
        call.sessions.set(newSession)
        call.respond(HttpStatusCode.OK)
    }

    get("/_testing/csrfToken") {
        if (call.attributes.contains(AttributeKey<Unit>("StatusPagesTriggered"))) {
            return@get
        }

        call.sessionManager.ensureCsrf()

        call.respondText(call.sessions.get<SessionData>()!!.csrfToken)
    }
}
