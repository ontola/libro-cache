package tools.empathy.libro.server.routes

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import tools.empathy.libro.server.plugins.sessionManager
import tools.empathy.libro.server.sessions.logout

suspend fun PipelineContext<Unit, ApplicationCall>.handleLogout() {
    if (call.attributes.contains(AttributeKey<Unit>("StatusPagesTriggered"))) {
        return
    }

    call.response.header(HttpHeaders.Vary, "Accept,Accept-Encoding,Content-Type")

    val response = logout() ?: return call.respond(HttpStatusCode.BadRequest)

    if (response.status == HttpStatusCode.OK) {
        call.sessionManager.delete()
    }

    call.respond(response.status)
}

fun Routing.mountLogout() {
    get("/*/logout") { handleLogout() }
    get("/logout") { handleLogout() }
    post("/*/logout") { handleLogout() }
    post("/logout") { handleLogout() }
}
