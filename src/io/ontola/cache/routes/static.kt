package io.ontola.cache.routes

import io.ktor.http.content.CompressedFileType
import io.ktor.http.content.files
import io.ktor.http.content.preCompressed
import io.ktor.http.content.static
import io.ktor.routing.Routing

fun Routing.mountStatic() {
    static("f_assets") {
        preCompressed(CompressedFileType.BROTLI, CompressedFileType.GZIP) {
            files("assets")
        }
    }
}
