package io.ontola.cache.document

import io.ktor.application.ApplicationCall
import io.ktor.http.Url
import io.ontola.cache.assets.assets
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.nonce
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.tenantization.Manifest
import io.ontola.cache.tenantization.tenant
import io.ontola.cache.util.requestUriFromTenant
import kotlinx.serialization.json.Json

data class PageRenderContext(
    val uri: Url,
    val nonce: String,
    val lang: String,
    val isUser: Boolean,
    val manifest: Manifest,
    val configuration: PageConfiguration,
    val serializer: Json,
    var seed: String? = null,
)

fun ApplicationCall.pageRenderContextFromCall(
    seed: String? = null,
    manifest: Manifest? = null,
) = PageRenderContext(
    uri = requestUriFromTenant(),
    nonce = nonce,
    lang = sessionManager.language,
    isUser = sessionManager.isUser,
    manifest = manifest ?: tenant.manifest,
    configuration = PageConfiguration(
        appElement = "root",
        assets = application.assets,
    ),
    serializer = application.cacheConfig.serializer,
    seed = seed,
)
