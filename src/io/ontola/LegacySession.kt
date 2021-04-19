package io.ontola

import kotlinx.serialization.Serializable

@Serializable
data class LegacySession(
    val userToken: String? = null,
    val refreshToken: String? = null,
    val secret: String? = null,
    val count: Long = 0,
    val _expire: Long = 0,
    val _maxAge: Long = 0,
)
