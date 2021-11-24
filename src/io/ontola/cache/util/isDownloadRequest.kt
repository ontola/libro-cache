package io.ontola.cache.util

import io.ktor.http.Url

fun Url.isDownloadRequest() = parameters.contains("download", "true")
