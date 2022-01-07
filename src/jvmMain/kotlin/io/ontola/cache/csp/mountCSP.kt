package io.ontola.cache.csp

import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ontola.cache.plugins.cacheConfig

class CSPReportException(override val message: String?) : Exception(message)

const val cspReportEndpointPath = "/csp-reports"

fun Routing.mountCSP() {
    post(cspReportEndpointPath) {
        val body = call.receiveText()
        application.cacheConfig.notify(CSPReportException(body))
    }
}
