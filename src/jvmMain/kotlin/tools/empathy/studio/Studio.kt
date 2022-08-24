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
import tools.empathy.url.absolutize
import tools.empathy.url.appendPath
import tools.empathy.url.asHrefString
import tools.empathy.url.filename
import tools.empathy.url.fullUrl
import tools.empathy.url.origin
import tools.empathy.url.stem
import tools.empathy.url.withTrailingSlash

sealed class PublicationResult {
    object NoMatch : PublicationResult()
    object NotFound : PublicationResult()

    class Present(val distribution: Distribution) : PublicationResult()
}

private fun requestUri(call: ApplicationCall): Url {
    val origin = call.request.origin

    return URLBuilder(
        protocol = URLProtocol.createOrDefault(origin.scheme),
        host = origin.host,
        port = origin.port.onlyNonDefaultPort(),
        pathSegments = call.request.path().split("/").filter { it.isNotBlank() },
    ).apply {
        parameters.appendAll(call.request.queryParameters)
    }.build()
}

private fun Int.onlyNonDefaultPort(): Int {
    if (this == 80 || this == 443) {
        return DEFAULT_PORT
    }

    return this
}

private val logger = KotlinLogging.logger {}

val StudioDeploymentKey = AttributeKey<PageRenderContext>("StudioDeploymentKey")

fun studioManifest(url: Url): Manifest = Manifest.forWebsite(Url(url.origin())).copy(
    name = "Studio",
    shortName = "Studio",
)

@OptIn(ExperimentalSerializationApi::class)
val Studio = createApplicationPlugin(name = "Studio", ::StudioConfiguration) {
    pluginConfig.complete(application)
    val xmlFormatter = XML {
        xmlDeclMode = XmlDeclMode.Minimal
        xmlVersion = XmlVersion.XML10
        indent = 4
    }
    val staticDistributions by lazy { readStaticDistributions() }

    logger.error { "Static distributions: ${staticDistributions.keys}" }

    suspend fun findStaticDistribution(call: ApplicationCall, uri: Url): PublicationResult = call.measured("studioLookupStatic") {
        val stemmed = uri.stem()

        val distributions = if (call.application.libroConfig.isDev) {
            readStaticDistributions()
        } else {
            staticDistributions
        }

        val match = distributions.keys.find {
            it.toString() == stemmed || stemmed.startsWith(it.withTrailingSlash)
        }

        val distributionData = distributions[match]

        if (distributionData != null) {
            PublicationResult.Present(distributionData)
        } else {
            PublicationResult.NoMatch
        }
    }

    suspend fun findDatabaseDistribution(call: ApplicationCall, uri: Url): PublicationResult = call.measured("studioLookupDb") {
        val publication = pluginConfig.publicationRepo.match(uri)

        publication ?: return@measured PublicationResult.NoMatch

        logger.debug { "Prefix match for project ${publication.projectId} and distribution ${publication.distributionId}" }

        val distribution = pluginConfig.distributionRepo.get(publication.projectId, publication.distributionId)
            ?: return@measured PublicationResult.NotFound

        PublicationResult.Present(distribution)
    }

    suspend fun findDistribution(call: ApplicationCall, uri: Url): PublicationResult {
        val static = findStaticDistribution(call, uri)
        if (static is PublicationResult.Present) {
            return static
        }

        return findDatabaseDistribution(call, uri)
    }

    suspend fun hostStudio(call: ApplicationCall) {
        val uri = call.fullUrl()

        if (uri == Url(uri.origin())) {
            return call.respondRedirect(uri.appendPath("libro", "studio").toString())
        }

        val ctx = call.pageRenderContextFromCall(
            data = null,
            manifest = studioManifest(uri),
            uri = uri,
        )
        call.attributes.put(StudioDeploymentKey, ctx)
    }

    suspend fun intercept(call: ApplicationCall) {
        if (call.blacklisted) {
            return
        }

        val uri = requestUri(call)

        val distribution = when (val result = findDistribution(call, uri)) {
            PublicationResult.NoMatch -> return
            PublicationResult.NotFound -> return call.respond(HttpStatusCode.NotFound)
            is PublicationResult.Present -> {
                result.distribution
            }
        }
        val record = distribution.data[uri.asHrefString] ?: distribution.data[distribution.manifest.ontola.websiteIRI.absolutize(uri)]
        val ctx = call.pageRenderContextFromCall(
            data = distribution.data,
            manifest = distribution.manifest,
            uri = uri,
        )
        call.attributes.put(StudioDeploymentKey, ctx)

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
