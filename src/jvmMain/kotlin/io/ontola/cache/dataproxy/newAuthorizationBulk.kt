package io.ontola.cache.dataproxy

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ontola.cache.util.Actions
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.hasAction
import io.ontola.cache.util.setActionParam
import io.ontola.cache.util.setParameter
import mu.KotlinLogging

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
    val newAuthorization = response.headers[CacheHttpHeaders.NewAuthorization] ?: return null
    val refreshToken = response.headers[CacheHttpHeaders.NewRefreshToken]
        ?: throw Exception("Received New-Authorization header without New-Refresh-Header")


    return BulkControls(
        newAuthorization,
        refreshToken,
        responseAction(response),
    )
}
