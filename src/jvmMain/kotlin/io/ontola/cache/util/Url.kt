package io.ontola.cache.util

import io.ktor.http.Url
import io.ktor.http.authority
import io.ktor.http.fullPath
import io.ktor.http.hostWithPort

fun Url.filename(): String? = encodedPath.split("/").lastOrNull()

fun Url.origin(): String = "${this.protocol.name}://${this.authority}"

fun Url.stem(): String = "${origin()}$encodedPath"

fun Url.withoutProto(): String = "${origin().drop(toString().indexOf(authority))}$encodedPath"

val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort

val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"
