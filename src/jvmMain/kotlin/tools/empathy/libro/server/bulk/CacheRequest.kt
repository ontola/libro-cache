package tools.empathy.libro.server.bulk

import kotlinx.serialization.Serializable

@Serializable
open class CacheRequest(
    open val iri: String,
)
