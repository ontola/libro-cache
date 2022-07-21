package io.ontola.cache.tenantization

import io.ktor.client.plugins.ServerResponseException
import io.ktor.server.application.ApplicationCall
import io.ontola.cache.util.LibroHttpHeaders

/**
 * Retrieves the value from the version header of the connected backend.
 */
internal suspend fun ApplicationCall.getApiVersion(): String? = try {
    getTenantsRequest().headers[LibroHttpHeaders.XAPIVersion]
} catch (e: ServerResponseException) {
    null
}
