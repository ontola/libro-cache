package tools.empathy.url

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.authority
import io.ktor.http.fullPath
import io.ktor.http.hostWithPort

fun Url.appendPath(path: String): Url = appendPath(*path.split("/").toTypedArray())

fun Url.appendPath(vararg segments: String): Url {
    return URLBuilder(this).apply {
        pathSegments = pathSegments
            // TODO: Remove after https://youtrack.jetbrains.com/issue/KTOR-3618 is fixed
            .filter { it.isNotBlank() }
            .toMutableList()
            .apply { addAll(segments.filter { it.isNotBlank() }) }
    }.build()
}

fun Url.filename(): String? = encodedPath.split("/").lastOrNull()

fun Url.origin(): String = "${protocol.name}://$authority"

fun Url.stem(): String = if (encodedPath === "") "${origin()}/" else "${origin()}$encodedPath"

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

fun Url?.absolutize(other: Url): String = absolutize(other.toString())

fun Url?.absolutize(other: String): String {
    this ?: return other

    val prefix = withoutTrailingSlash

    if (other == prefix)
        return "/"
    if (other.startsWith(prefix))
        return other.removePrefix(prefix)

    return other
}

/** Ensures a path is present and trailing paths are trimmed */
val Url.asHref
    get() = if (this.pathSegments.size > 1) Url(this.withoutTrailingSlash) else this

/** Ensures a path is present and trailing paths are trimmed */
val Url.asHrefString
    get() = asHref.toString()

val Url.withoutTrailingSlash
    get() = this.toString().trimEnd('/')

val Url.withTrailingSlash
    get() = "$withoutTrailingSlash/"
