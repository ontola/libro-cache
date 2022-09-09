package tools.empathy.libro.server.util

import io.ktor.server.sessions.CookieConfiguration

fun CookieConfiguration.configure() {
    httpOnly = true
    secure = true
    extensions["SameSite"] = "Lax"
}
