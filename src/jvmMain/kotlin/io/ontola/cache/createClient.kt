package io.ontola.cache

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.ontola.cache.util.configureClientLogging
import io.ontola.util.disableCertValidation
import kotlinx.serialization.json.Json

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
    install(UserAgent) { agent = "cache" }
}

fun createClient(production: Boolean): HttpClient = HttpClient(CIO) {
    configureClient()
    if (!production) {
        disableCertValidation()
    }
}
