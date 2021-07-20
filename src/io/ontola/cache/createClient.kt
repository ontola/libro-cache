package io.ontola.cache

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.UserAgent
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.Logging
import io.ontola.cache.util.configureClientLogging

val configureClient: HttpClientConfig<*>.() -> Unit = {
    install(Auth) {}
    install(JsonFeature) {
        serializer = KotlinxSerializer()
    }
    install(Logging) {
        configureClientLogging()
    }
    install(UserAgent) { agent = "cache" }
}

fun createClient(): HttpClient = HttpClient(CIO, configureClient)
