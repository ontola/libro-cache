package io.ontola.cache.statuspages

import io.ktor.http.HttpStatusCode
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.title

fun titleForStatus(status: HttpStatusCode): String {
    return when (status) {
        HttpStatusCode.BadGateway -> "Bad gateway"
        HttpStatusCode.Forbidden -> "This item is hidden"
        HttpStatusCode.NotFound -> "This item is not found"
        HttpStatusCode.Unauthorized -> "Unauthorized"
        else -> "Internal server error"
    }
}

fun bodyForStatus(status: HttpStatusCode): String {
    return when (status) {
        HttpStatusCode.BadGateway -> "There was a networking issue during this request, please retry or try again later."
        HttpStatusCode.Forbidden -> "Maybe it's visible after logging in."
        HttpStatusCode.NotFound -> "Maybe the item you are looking for is deleted or never existed."
        HttpStatusCode.Unauthorized -> "You have to be logged in to view this resource."
        else -> "An error occurred on our side, please try again later."
    }
}

fun HTML.errorPage(status: HttpStatusCode) {
    head {
        meta(charset = "utf-8")
        title(titleForStatus(status))
    }
    body {
        h1 { +titleForStatus(status) }
        p { +bodyForStatus(status) }
    }
}
