package io.ontola.cache.bulk

import kotlinx.serialization.Serializable

@Serializable
open class CacheRequest(
    open val iri: String,
)
