package io.ontola.apex.webmanifest

import kotlinx.serialization.Serializable

@Serializable
data class Icon(
    val src: String,
    val sizes: String,
    val type: String,
    val purpose: String? = null,
)
