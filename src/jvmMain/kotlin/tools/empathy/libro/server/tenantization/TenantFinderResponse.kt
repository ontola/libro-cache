package tools.empathy.libro.server.tenantization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Response of the `find_tenant` SPI endpoint.
 */
@Serializable
data class TenantFinderResponse(
    val uuid: String? = null,
    @SerialName("all_shortnames")
    val allShortnames: List<String> = emptyList(),
    /**
     * The host and possible path of the website.
     */
    @SerialName("iri_prefix")
    val iriPrefix: String,
    @SerialName("header_background")
    val headerBackground: String? = null,
    @SerialName("header_text")
    val headerText: String? = null,
    @SerialName("language")
    val language: String? = null,
    @SerialName("secondary_color")
    val secondaryColor: String? = "#d96833",
    @SerialName("primary_color")
    val primaryColor: String? = "#475668",
    @SerialName("database_schema")
    val databaseSchema: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
) {
    @Transient
    lateinit var websiteBase: String
}
