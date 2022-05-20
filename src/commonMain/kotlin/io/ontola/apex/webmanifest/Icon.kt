package io.ontola.apex.webmanifest

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Icon(
    val src: String,
    val sizes: String,
    val type: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val purpose: String? = null,
)
