package io.ontola.libro.metadata

import io.ktor.http.Url

data class TagProps(
    val children: String? = null,
    val content: String? = null,
    val href: Url? = null,
    val id: String? = null,
    val itemProp: String? = null,
    val name: String? = null,
    val property: String? = null,
    val rel: String? = null,
    val type: String,
)
