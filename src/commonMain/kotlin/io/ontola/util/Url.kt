package io.ontola.util

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.authority
import io.ktor.http.fullPath
import io.ktor.http.hostWithPort

fun Url.appendPath(path: String): Url = appendPath(*path.split("/").toTypedArray())

fun Url.appendPath(vararg segments: String): Url = URLBuilder(this)
    .apply { appendPathSegments(*segments) }
    .build()

fun Url.filename(): String? = encodedPath.split("/").lastOrNull()

fun Url.origin(): String = "${protocol.name}://$authority"

fun Url.stem(): String = "${origin()}$encodedPath"

fun Url.withoutProto(): String = "${origin().drop(toString().indexOf(authority))}$encodedPath"

val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort

val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"

/**
 * Rebase [fullPath] on [this], keeping any previous path segments and query parameters.
 */
fun Url.rebase(fullPath: String): Url = URLBuilder(this).apply {
    Url(fullPath).let {
        pathSegments = (pathSegments + it.pathSegments).filter(String::isNotBlank)
        encodedFragment = it.encodedFragment
        parameters.appendAll(it.parameters)
    }
}.build()

val Url.withoutTrailingSlash
    get() = this.toString().trimEnd('/')

val Url.withTrailingSlash
    get() = "$withoutTrailingSlash/"
