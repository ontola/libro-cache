package tools.empathy.libro.server.bulk

import kotlinx.serialization.Serializable

@Serializable
data class SPIResourceResponseItem(
    val iri: String,
    val status: Int,
    val cache: CacheControl,
    val language: String? = null,
    val body: String? = null,
)
