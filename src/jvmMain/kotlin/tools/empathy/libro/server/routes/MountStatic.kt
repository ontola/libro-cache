package tools.empathy.libro.server.routes

import io.ktor.server.http.content.CompressedFileType
import io.ktor.server.http.content.files
import io.ktor.server.http.content.preCompressed
import io.ktor.server.http.content.static
import io.ktor.server.routing.Routing

fun Routing.mountStatic() {
    static("f_assets") {
        preCompressed(CompressedFileType.BROTLI, CompressedFileType.GZIP) {
            files("assets")
        }
    }
}
