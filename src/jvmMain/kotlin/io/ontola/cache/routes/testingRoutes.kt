package io.ontola.cache.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.sessions.SessionData
import kotlinx.serialization.decodeFromString

fun Routing.mountTestingRoutes() {
    post("/_testing/setSession") {
        val newSession = call.application.cacheConfig.serializer.decodeFromString<SessionData>(call.receive<String>())
        call.sessions.set(newSession)
        call.respond(HttpStatusCode.OK)
    }
}
