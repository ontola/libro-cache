package io.ontola.cache.document

import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.bundle.bundles
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.language
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.tenantization.tenant
import io.ontola.cache.util.requestUriFromTenant
import io.ontola.empathy.web.DataSlice
import kotlinx.serialization.json.Json

data class PageRenderContext(
    val uri: Url,
    val lang: String,
    val isUser: Boolean,
    val csrfToken: String,
    val manifest: Manifest,
    val configuration: PageConfiguration,
    val serializer: Json,
    var data: DataSlice?,
) {
    lateinit var nonce: String
}

fun ApplicationCall.pageRenderContextFromCall(
    data: DataSlice? = null,
    manifest: Manifest? = null,
    uri: Url = requestUriFromTenant(),
): PageRenderContext {
    sessionManager.ensureCsrf()

    return PageRenderContext(
        uri = uri,
        lang = language,
        isUser = sessionManager.isUser,
        csrfToken = sessionManager.session!!.csrfToken,
        manifest = manifest ?: tenant.manifest,
        configuration = PageConfiguration(
            appElement = "root",
            bundles = bundles,
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
