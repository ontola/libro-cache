package tools.empathy.libro.server.document

import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.bundle.bundles
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.plugins.language
import tools.empathy.libro.server.plugins.sessionManager
import tools.empathy.libro.server.tenantization.tenant
import tools.empathy.libro.server.util.requestUriFromTenant
import tools.empathy.libro.webmanifest.Manifest
import tools.empathy.serialization.DataSlice

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
            tileServerUrl = application.libroConfig.maps?.mapboxTileURL,
            bugsnagOpts = application.libroConfig.clientReportingKey?.let {
                BugsnagOpts(
                    apiKey = it,
                    releaseStage = application.libroConfig.env,
                    appVersion = application.libroConfig.clientVersion,
                )
            },
        ),
        serializer = application.libroConfig.serializer,
        data = data,
    )
}
