package io.ontola.cache.statuspages

import io.ktor.http.HttpStatusCode
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.meta
import kotlinx.html.title

fun HTML.errorPage(status: HttpStatusCode) {
    head {
        meta(charset = "utf-8")
        title("Error")
    }
    body {
        h1 { +"Error" }
    }
}