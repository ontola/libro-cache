package io.ontola.cache.dataproxy

import io.ktor.application.ApplicationCall
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.util.Actions
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.hasAction
import io.ontola.cache.util.setActionParam
import io.ontola.cache.util.setParameter

internal fun ApplicationCall.newAuthorizationBulk(response: HttpResponse): String? {
    val newAuthorization = response.headers[CacheHttpHeaders.NewAuthorization] ?: return null

    sessionManager.setAuthorization(
        accessToken = newAuthorization,
        refreshToken = response.headers[CacheHttpHeaders.NewRefreshToken]
            ?: throw Exception("Received New-Authorization header without New-Refresh-Header"),
    )

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
