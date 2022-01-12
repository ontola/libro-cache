package io.ontola.cache.tenantization

import io.ktor.client.HttpClient
import io.ktor.http.Url
import io.ontola.apex.webmanifest.Manifest

data class TenantData(
    internal val client: HttpClient,
    val isBlackListed: Boolean,
    val websiteIRI: Url,
    val websiteOrigin: Url,
    val manifest: Manifest,
)
