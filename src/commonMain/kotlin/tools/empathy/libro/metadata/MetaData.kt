package tools.empathy.libro.metadata

import io.ktor.http.Url

/**
 * Metadata about the current page.
 */
data class MetaData(
    val appIcon: String? = null,
    val appName: String?,
    val name: String? = null,
    val url: Url,
    val text: String? = null,
    val coverURL: String? = null,
    val imageURL: String? = null,
)
