package tools.empathy.libro.server.configuration

import io.ktor.http.Url

data class StudioConfig(
    val skipAuth: Boolean,
    val domain: String,
    val origin: Url = Url("https://$domain"),
)
