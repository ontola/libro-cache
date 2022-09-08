package tools.empathy.libro.webmanifest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tracking(
    val host: String? = null,
    val cdn: String? = "https://$host/",
    val type: TrackerType,
    @SerialName("container_id")
    val containerId: String,
)
