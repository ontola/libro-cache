package tools.empathy.libro.server.util

import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLBuilder

object Actions {
    const val RedirectAction = "https://ns.ontola.io/libro/actions/redirect"
    const val RefreshAction = "https://ns.ontola.io/libro/actions/refresh"
}

const val actionSeparator = ", "

/**
 * The list of actions provided by the backend for the client to execute.
 */
fun HttpResponse.actions(): List<String> =
    headers[LibroHttpHeaders.ExecAction]?.split(actionSeparator) ?: emptyList()

/**
 * Retrieve an action from the [HttpResponse] if present.
 */
fun HttpResponse.getAction(wanted: String): String? =
    actions().find { it.startsWith(wanted) }

/**
 * Check if an action is present.
 */
fun HttpResponse.hasAction(wanted: String): Boolean = getAction(wanted) != null

/**
 * Update an action with a query parameter.
 * Will be ignored when the action isn't present.
 *
 * @param action The id of the action.
 * @param param The name of the parameter.
 * @param value The value of the parameter.
 */
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
