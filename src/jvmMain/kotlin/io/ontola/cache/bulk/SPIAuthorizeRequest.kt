package io.ontola.cache.bulk

import kotlinx.serialization.Serializable

@Serializable
data class SPIAuthorizeRequest(
    val resources: List<SPIResourceRequestItem>,
)
