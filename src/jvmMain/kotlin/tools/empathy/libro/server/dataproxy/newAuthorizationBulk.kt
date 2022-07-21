package tools.empathy.libro.server.dataproxy

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import mu.KotlinLogging
import tools.empathy.libro.server.util.Actions
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.libro.server.util.hasAction
import tools.empathy.libro.server.util.setActionParam
import tools.empathy.libro.server.util.setParameter

val logger = KotlinLogging.logger {}

data class BulkControls(
    val accessToken: String,
    val refreshToken: String,
    val action: String?,
)

private fun responseAction(response: HttpResponse): String? {
    val isRedirect = (response.status.value in 300..399)

    if (isRedirect) {
        return null
    }

    if (response.hasAction(Actions.RedirectAction)) {
        return response.setActionParam(Actions.RedirectAction, "reload", "true")
    }

    val location = response.headers[HttpHeaders.Location]
    if (location != null && location.isNotEmpty()) {
        return Actions.RedirectAction
            .setParameter("reload", "true")
            .setParameter("location", location)
    }

    return Actions.RefreshAction
}

internal fun newAuthorizationBulk(response: HttpResponse): BulkControls? {
    val newAuthorization = response.headers[LibroHttpHeaders.NewAuthorization] ?: return null
    val refreshToken = response.headers[LibroHttpHeaders.NewRefreshToken]
        ?: throw Exception("Received New-Authorization header without New-Refresh-Header")

    return BulkControls(
        newAuthorization,
        refreshToken,
        responseAction(response),
    )
}
