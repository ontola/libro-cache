package io.ontola.cache.sessions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LogoutRequest(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("client_secret")
    val clientSecret: String,
    val token: String,
)
