package io.ontola.cache.bulk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CacheControl {
    @SerialName("none")
    None,
    @SerialName("public")
    Public,
    @SerialName("private")
    Private,
    @SerialName("no-cache")
    NoCache,
}
