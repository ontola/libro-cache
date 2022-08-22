package tools.empathy.libro.server.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header
import io.ktor.server.request.host
import io.ktor.server.request.port
import io.ktor.server.response.respondRedirect
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.document.pageRenderContextFromCall
import tools.empathy.libro.server.landing.landingSite
import tools.empathy.libro.server.tenantization.TenantData
import tools.empathy.libro.webmanifest.Icon
import tools.empathy.libro.webmanifest.Manifest
import tools.empathy.vocabularies.LibroData
import tools.empathy.vocabularies.OntolaData
import tools.empathy.vocabularies.SchemaData

fun managementTenant(origin: Url, port: Int): TenantData.Local {
    val managementManifest = Manifest.forWebsite(origin).let {
        it.copy(
            name = "Local",
            icons = arrayOf(
                Icon(
                    src = "/f_assets/images/libro-logo-t-4.svg",
                    sizes = "32x32 64x64 72x72 96x96 128x128",
                    purpose = "favicon",
                    type = "image/svg",
                ),
            ),
            ontola = it.ontola.copy(
                primaryColor = "#002233",
            ),
        )
    }

    return TenantData.Local(
        name = "Libro Management",
        websiteIRI = origin,
        websiteOrigin = origin,
        allowUnsafe = true,
        unsafePort = port,
        context = {
            pageRenderContextFromCall(
                data = landingSite() + SchemaData + LibroData + OntolaData,
                manifest = managementManifest,
            )
        },
        manifest = managementManifest,
    )
}

val DevelopmentSupport = createApplicationPlugin("DevelopmentSupport") {
    onCall { call ->
        val config = call.application.libroConfig
        if (config.isDev) {
            val isLocalHost = call.request.host() == "localhost" && !arrayOf(80, 443).contains(call.request.port())
            val isLocalDevelopment = !call.request.behindProxy() && isLocalHost

            if (isLocalDevelopment && call.request.port() == config.port) {
                call.respondRedirect(config.management.origin.toString(), false)
            }
        }
    }
}

private fun ApplicationRequest.behindProxy(): Boolean {
    val proxyHeader = header(HttpHeaders.Forwarded)
        ?: header(HttpHeaders.XForwardedFor)
        ?: header(HttpHeaders.XForwardedHost)
        ?: header(HttpHeaders.XForwardedProto)

    return proxyHeader != null
}
