package tools.empathy.libro.server.bulk

import kotlinx.serialization.Serializable

@Serializable
data class SPIAuthorizeRequest(
    val resources: List<SPIResourceRequestItem>,
)
