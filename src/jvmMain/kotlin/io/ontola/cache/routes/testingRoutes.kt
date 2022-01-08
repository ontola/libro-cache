package io.ontola.cache.routes

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
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.sessions.SessionData
import kotlinx.serialization.decodeFromString

fun Routing.mountTestingRoutes() {
    post("/_testing/setSession") {
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
