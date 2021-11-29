package io.ontola.cache.tenantization

import io.ktor.client.HttpClient
import io.ktor.http.Url

data class TenantData(
    internal val client: HttpClient,
    val isBlackListed: Boolean,
    val websiteIRI: Url,
    val websiteOrigin: Url,
    val currentIRI: Url,
    val manifest: Manifest,
)
