package tools.empathy.studio

import io.ktor.http.ContentType
import io.ktor.http.DEFAULT_PORT
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.plugins.origin
import io.ktor.server.request.host
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.util.AttributeKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToStream
import mu.KotlinLogging
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.document.PageRenderContext
import tools.empathy.libro.server.document.pageRenderContextFromCall
import tools.empathy.libro.server.plugins.blacklisted
import tools.empathy.libro.server.plugins.setManifestLanguage
import tools.empathy.libro.server.util.measured
import tools.empathy.libro.webmanifest.Manifest
import tools.empathy.serialization.translations
import tools.empathy.url.filename
import tools.empathy.url.fullUrl
import tools.empathy.url.origin
import tools.empathy.url.withoutTrailingSlash

private fun Int.onlyNonDefaultPort(): Int {
    if (this == 80 || this == 443) {
        return DEFAULT_PORT
    }

    return this
}

private val logger = KotlinLogging.logger {}

val StudioDeploymentKey = AttributeKey<PageRenderContext>("StudioDeploymentKey")

@OptIn(ExperimentalSerializationApi::class)
val Studio = createApplicationPlugin(name = "Studio", ::StudioConfiguration) {
    pluginConfig.complete(application)
    val xmlFormatter = XML {
        xmlDeclMode = XmlDeclMode.Minimal
        xmlVersion = XmlVersion.XML10
        indent = 4
    }

    fun hostStudio(call: ApplicationCall) {
        val uri = call.fullUrl()

        val ctx = call.pageRenderContextFromCall(
            data = null,
            manifest = Manifest.forWebsite(Url(uri.origin())).copy(
                name = "Studio",
            ),
            uri = uri,
        )
        call.attributes.put(StudioDeploymentKey, ctx)
    }

    suspend fun intercept(call: ApplicationCall) {
        if (call.blacklisted) {
            return
        }

        lateinit var uri: Url
        val publication = call.measured("studioLookup") {
            val origin = call.request.origin
            uri = URLBuilder(
                protocol = URLProtocol.createOrDefault(origin.scheme),
                host = origin.host,
                port = origin.port.onlyNonDefaultPort(),
                pathSegments = call.request.path().split("/").filter { it.isNotBlank() },
            ).apply {
                parameters.appendAll(call.request.queryParameters)
            }.build()

            pluginConfig.publicationRepo.match(uri)
        }

        publication ?: return

        logger.debug { "Prefix match for project ${publication.projectId} and distribution ${publication.distributionId}" }

        val distribution = pluginConfig.distributionRepo.get(publication.projectId, publication.distributionId)
            ?: return call.respond(HttpStatusCode.NotFound)

        val record = distribution.data[uri.withoutTrailingSlash]

        if (uri.filename() == "manifest.json") {
            call.respondOutputStream(ContentType.Application.Json) {
                application.libroConfig.serializer.encodeToStream(distribution.manifest, this)
            }
        } else if (uri.filename() == "sitemap.txt") {
            call.respondText(distribution.sitemap)
        } else if (uri.filename() == "sitemap.xml") {
            call.respondText(
                text = xmlFormatter.encodeToString(distribution.xmlSitemap),
                contentType = ContentType.Application.Xml,
            )
        } else if (record == null) {
            call.respond(HttpStatusCode.NotFound)
        } else if (!record.translations().isNullOrEmpty()) {
            call.respondRedirect(record.translations()!!.first().value, permanent = false)
        } else {
            record["_language"]?.firstOrNull()?.value?.let { call.setManifestLanguage(it) }
            val ctx = call.pageRenderContextFromCall(
                data = distribution.data,
                manifest = distribution.manifest,
                uri = uri,
            )

            call.attributes.put(StudioDeploymentKey, ctx)
        }
    }

    onCall { call ->
        if (call.request.host() == pluginConfig.studioConfig.domain) {
            hostStudio(call)
        } else {
            intercept(call)
        }
    }
}
