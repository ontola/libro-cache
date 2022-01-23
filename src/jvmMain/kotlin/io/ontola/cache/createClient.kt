package io.ontola.cache

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.json.JsonPlugin
import io.ktor.client.plugins.json.serializer.KotlinxSerializer
import io.ktor.client.plugins.logging.Logging
import io.ontola.cache.util.configureClientLogging
import kotlinx.serialization.json.Json

val configureClient: HttpClientConfig<*>.() -> Unit = {
    followRedirects = false
    install(JsonPlugin) {
        serializer = KotlinxSerializer(
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

fun createClient(): HttpClient = HttpClient(CIO, configureClient)
