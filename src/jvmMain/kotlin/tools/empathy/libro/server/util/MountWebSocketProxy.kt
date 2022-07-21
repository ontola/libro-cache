package tools.empathy.libro.server.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.util.AttributeKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import tools.empathy.libro.server.plugins.services
import tools.empathy.libro.server.plugins.sessionManager
import tools.empathy.libro.server.tenantization.tenant
import tools.empathy.url.origin

fun Route.mountWebSocketProxy() {
    webSocket(path = "/{prefix?}/cable", protocol = "actioncable-v1-json") {
        handleWebsocket()
    }
    webSocket(path = "/cable", protocol = "actioncable-v1-json") {
        handleWebsocket()
    }
}

private suspend fun DefaultWebSocketServerSession.handleWebsocket() {
    if (call.attributes.contains(AttributeKey<Unit>("StatusPagesTriggered"))) {
        return
    }

    val serverSession = this

    val client = HttpClient(CIO).config {
        install(WebSockets)
    }

    val webSocketPath = call.tenant.manifest.ontola.websocketPath ?: return call.respond(HttpStatusCode.ExpectationFailed)
    val websitePath = call.tenant.websiteIRI.encodedPath
    val fullPath = "${websitePath.trimEnd('/')}/$webSocketPath"
    val service = call.services.route(fullPath)

    client.webSocket(
        call.request.httpMethod,
        host = service.host,
        port = service.port,
        path = fullPath,
        request = {
            url.protocol = URLProtocol.WS
            println("Connecting to: ${url.buildString()}")

            headers {
                proxySafeHeaders(call.request)
                header(HttpHeaders.Origin, call.tenant.websiteIRI.origin())
                serverSession.call.sessionManager.session?.let {
                    header(HttpHeaders.Authorization, it.accessTokenBearer())
                }
            }
        },
    ) {
        val clientSession = this

        val serverJob = launch(Dispatchers.IO) {
            for (received in serverSession.incoming) {
                clientSession.send(received)
            }
        }

        val clientJob = launch(Dispatchers.IO) {
            for (received in clientSession.incoming) {
                serverSession.send(received)
            }
        }

        joinAll(serverJob, clientJob)
    }
}
