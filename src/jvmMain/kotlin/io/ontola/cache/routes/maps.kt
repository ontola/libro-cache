package io.ontola.cache.routes

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.util.AttributeKey
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
        if (call.attributes.contains(AttributeKey<Unit>("StatusPagesTriggered"))) {
            return@get
        }

        val mapsConfig = call.application.cacheConfig.maps ?: error("Maps service credentials not configured")

        val token = call.createMapsToken(mapsConfig)

        call.respond(token)
    }
}

private suspend fun ApplicationCall.createMapsToken(mapsConfig: MapsConfig): AccessTokenResponse {
    val config = application.cacheConfig
    val expiresAt = Clock.System.now().plus(1.hours).toString()

    val response = config.client.post(mapsConfig.tokenEndpoint) {
        setBody(
            AccessTokenRequest(
                scopes = mapsConfig.scopes,
                expires = expiresAt,
            )
        )
    }

    val token = response.body<AccessTokenResponse>()

    if (!token.accessToken.startsWith("tk.")) {
        throw Exception("Map access token not temporary")
    }

    return token
}
