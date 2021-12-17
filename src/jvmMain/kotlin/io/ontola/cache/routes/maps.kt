package io.ontola.cache.routes

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.call.receive
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ontola.cache.plugins.MapsConfig
import io.ontola.cache.plugins.cacheConfig
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

data class AccessTokenRequest(
    val scopes: List<String>,
    val expires: String,
)

data class AccessTokenResponse(
    val accessToken: String,
    val expiresAt: String,
)

fun Routing.mountMaps() {
    get("/api/maps/accessToken") {
        val mapsConfig = call.application.cacheConfig.maps ?: error("Maps service credentials not configured")

        val token = call.createMapsToken(mapsConfig)

        call.respond(token)
    }
}

private suspend fun ApplicationCall.createMapsToken(mapsConfig: MapsConfig): AccessTokenResponse {
    val config = application.cacheConfig
    val expiresAt = Clock.System.now().plus(1.hours).toString()

    val response = config.client.post<HttpResponse>(mapsConfig.tokenEndpoint) {
        body = AccessTokenRequest(
            scopes = mapsConfig.scopes,
            expires = expiresAt,
        )
    }

    val token = response.receive<AccessTokenResponse>()

    if (!token.accessToken.startsWith("tk.")) {
        throw Exception("Map access token not temporary")
    }

    return token
}
