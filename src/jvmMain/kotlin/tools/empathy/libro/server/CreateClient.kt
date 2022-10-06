package tools.empathy.libro.server

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.util.configureClientLogging
import tools.empathy.url.disableCertValidation

val configureClient: HttpClientConfig<*>.() -> Unit = {
    followRedirects = false
    install(ContentNegotiation) {
        json(
            Json {
                isLenient = false
                ignoreUnknownKeys = false
                encodeDefaults = true
            }
        )
    }
    install(HttpTimeout)
    install(Logging) {
        configureClientLogging()
    }
    install(UserAgent) { agent = "libro-server" }
}

fun createClient(production: Boolean): HttpClient = HttpClient(CIO) {
    configureClient()
    if (!production) {
        disableCertValidation()
    }
}
