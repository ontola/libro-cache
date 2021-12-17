package io.ontola.cache.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ontola.cache.bulk.bulkHandler
import io.ontola.cache.plugins.storage

fun Routing.mountBulk() {
    get("/link-lib/cache/status") {
        call.respondText("UP", contentType = ContentType.Text.Plain)
    }

    get("/link-lib/cache/clear") {
        val test = call.application.storage.clear()

        call.respondText(test ?: "no message given", ContentType.Text.Plain, HttpStatusCode.OK)
    }

    post(bulkHandler())
}
