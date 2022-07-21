package tools.empathy.libro.server.bulk

import kotlinx.serialization.Serializable

@Serializable
data class SPIResourceRequestItem(
    val iri: String,
    val include: Boolean,
)
