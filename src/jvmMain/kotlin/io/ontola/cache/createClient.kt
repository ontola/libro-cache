package io.ontola.cache

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.UserAgent
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.Logging
import io.ontola.cache.util.configureClientLogging
import kotlinx.serialization.json.Json

val configureClient: HttpClientConfig<*>.() -> Unit = {
    followRedirects = false
    install(JsonFeature) {
        serializer = KotlinxSerializer(
            Json {
                isLenient = false
                ignoreUnknownKeys = false
                encodeDefaults = true
            }
        )
    }
    install(Logging) {
        configureClientLogging()
    }
    install(UserAgent) { agent = "cache" }
}

fun createClient(): HttpClient = HttpClient(CIO, configureClient)
