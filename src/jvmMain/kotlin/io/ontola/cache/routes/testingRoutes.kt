package io.ontola.cache.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.sessions.sessions
import io.ktor.sessions.set
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
