package tools.empathy.libro.server.routes

import io.ktor.server.http.content.CompressedFileType
import io.ktor.server.http.content.default
import io.ktor.server.http.content.files
import io.ktor.server.http.content.preCompressed
import io.ktor.server.http.content.static
import io.ktor.server.http.content.staticRootFolder
import io.ktor.server.routing.Routing
import java.io.File

fun Routing.mountStatic(isDev: Boolean) {
    if (isDev) {
        static("/libro/docs") {
            staticRootFolder = File("build/dokka/html")
            files(".")
            default("index.html")
        }
    }

    static("f_assets") {
        preCompressed(CompressedFileType.BROTLI, CompressedFileType.GZIP) {
            files("resources/client")
            files("resources/static")
        }
    }
}
