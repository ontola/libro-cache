package io.ontola.cache.bulk

import kotlinx.serialization.Serializable

@Serializable
data class SPIResourceRequestItem(
    val iri: String,
    val include: Boolean,
)
