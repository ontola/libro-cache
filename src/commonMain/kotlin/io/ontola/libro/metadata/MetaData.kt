package io.ontola.libro.metadata

data class MetaData(
    val appIcon: String? = null,
    val appName: String?,
    val name: String? = null,
    val url: String,
    val text: String? = null,
    val coverURL: String? = null,
    val imageURL: String? = null,
)
