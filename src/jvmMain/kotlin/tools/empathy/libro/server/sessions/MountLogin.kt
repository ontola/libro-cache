package tools.empathy.libro.server.sessions

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get

fun Routing.mountLogin() {
    authenticate("solid-oidc") {
        get("/login") {
            // Redirects to 'authorizeUrl' automatically
            println("test")
        }
    }
}
