package io.ontola.cache.document

import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.assets.assets
import io.ontola.cache.csp.nonce
import io.ontola.cache.plugins.cacheConfig
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
    val csrfToken: String,
    val manifest: Manifest,
    val configuration: PageConfiguration,
    val serializer: Json,
    var data: List<Hextuple>?,
)

suspend fun ApplicationCall.pageRenderContextFromCall(
    data: List<Hextuple>? = null,
    manifest: Manifest? = null,
    uri: Url = requestUriFromTenant(),
): PageRenderContext {
    sessionManager.ensureCsrf()

    return PageRenderContext(
        uri = uri,
        nonce = nonce,
        lang = sessionManager.language,
        isUser = sessionManager.isUser,
        csrfToken = sessionManager.session!!.csrfToken,
        manifest = manifest ?: tenant.manifest,
        configuration = PageConfiguration(
            appElement = "root",
            assets = application.assets,
            tileServerUrl = application.cacheConfig.maps?.mapboxTileURL,
            bugsnagOpts = application.cacheConfig.clientReportingKey?.let {
                BugsnagOpts(
                    apiKey = it,
                    releaseStage = application.cacheConfig.env,
                    appVersion = application.cacheConfig.clientVersion,
                )
            },
        ),
        serializer = application.cacheConfig.serializer,
        data = data,
    )
}
