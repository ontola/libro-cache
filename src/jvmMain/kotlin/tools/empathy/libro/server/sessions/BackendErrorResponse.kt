package tools.empathy.libro.server.sessions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackendErrorResponse(
    val error: String,
    @SerialName("error_description")
    val errorDescription: String,
    val code: String? = null,
)
