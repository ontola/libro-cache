package io.ontola.cache.util

import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLBuilder

object Actions {
    const val RedirectAction = "https://ns.ontola.io/libro/actions/redirect"
    const val RefreshAction = "https://ns.ontola.io/libro/actions/refresh"
}

const val actionSeparator = ", "

fun HttpResponse.actions(): List<String> =
    headers[CacheHttpHeaders.ExecAction]?.split(actionSeparator) ?: emptyList()

fun HttpResponse.getAction(wanted: String): String? =
    actions().find { it.startsWith(wanted) }

fun HttpResponse.hasAction(wanted: String): Boolean = getAction(wanted) != null

fun HttpResponse.setActionParam(action: String, param: String, value: String): String {
    return actions().joinToString(actionSeparator) {
        if (it.startsWith(action)) {
            it.setParameter(param, value)
        } else {
            it
        }
    }
}

fun String.setParameter(param: String, value: String): String {
    return URLBuilder(this).apply {
        parameters[param] = value
    }.buildString()
}
