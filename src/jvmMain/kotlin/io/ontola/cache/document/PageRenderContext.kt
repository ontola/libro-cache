package io.ontola.cache.document

import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.assets.assets
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.nonce
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.tenantization.tenant
import io.ontola.cache.util.requestUriFromTenant
import io.ontola.rdf.hextuples.Hextuple
import kotlinx.serialization.json.Json

data class PageRenderContext(
    val uri: Url,
    val nonce: String,
    val lang: String,
    val isUser: Boolean,
    val manifest: Manifest,
    val configuration: PageConfiguration,
    val serializer: Json,
    var data: List<Hextuple>?,
)

fun ApplicationCall.pageRenderContextFromCall(
    data: List<Hextuple>? = null,
    manifest: Manifest? = null,
    uri: Url = requestUriFromTenant(),
) = PageRenderContext(
    uri = uri,
    nonce = nonce,
    lang = sessionManager.language,
    isUser = sessionManager.isUser,
    manifest = manifest ?: tenant.manifest,
    configuration = PageConfiguration(
        appElement = "root",
        assets = application.assets,
        tileServerUrl = application.cacheConfig.maps?.tokenEndpoint,
    ),
    serializer = application.cacheConfig.serializer,
    data = data,
)
