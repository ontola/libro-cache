package io.ontola.cache.util

import io.ktor.http.Url

/**
 * Check if the [Url] is marked as a download.
 *
 * Downloads should instruct the client to store the file rather than open it inline.
 */
fun Url.isDownloadRequest() = parameters.contains("download", "true")
