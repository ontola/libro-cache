package tools.empathy.libro.webmanifest

import kotlinx.serialization.Serializable

@Serializable
data class ServiceWorker(
    val src: String = "/f_assets/sw.js",
    val scope: String = "/",
)
