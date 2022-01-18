package io.ontola.cache.routes

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.util.AttributeKey
import io.ontola.cache.plugins.MapsConfig
import io.ontola.cache.plugins.cacheConfig
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.hours

@Serializable
data class AccessTokenRequest(
    val scopes: List<String>,
    val expires: String,
)

@Serializable
data class AccessTokenResponse(
    val expiresAt: String? = null,
    val token: String,
)

fun Routing.mountMaps() {
    get("/api/maps/accessToken") {
        if (call.attributes.contains(AttributeKey<Unit>("StatusPagesTriggered"))) {
            return@get
        }

        val mapsConfig = call.application.cacheConfig.maps ?: error("Maps service credentials not configured")

        val token = call.createMapsToken(mapsConfig)

        call.respondText(Json.encodeToString(token))
    }
}

private suspend fun ApplicationCall.createMapsToken(mapsConfig: MapsConfig): AccessTokenResponse {
    val config = application.cacheConfig
    val expiresAt = Clock.System.now().plus(1.hours).toString()

    val response = config.client.post(mapsConfig.tokenEndpoint) {
        contentType(ContentType.Application.Json)
        setBody(
            AccessTokenRequest(
                scopes = mapsConfig.scopes,
                expires = expiresAt,
            )
        )
    }

    val token = response.body<AccessTokenResponse>().copy(expiresAt = expiresAt)

    if (!token.token.startsWith("tk.")) {
        throw Exception("Map access token not temporary")
    }

    return token
}
