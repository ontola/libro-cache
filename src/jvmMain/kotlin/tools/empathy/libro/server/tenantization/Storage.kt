package tools.empathy.libro.server.tenantization

import io.ktor.http.Url
import io.ktor.server.application.Application
import io.ktor.server.request.ApplicationRequest
import kotlinx.serialization.decodeFromString
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.plugins.LookupKeys
import tools.empathy.libro.server.plugins.persistentStorage
import tools.empathy.libro.webmanifest.Manifest
import tools.empathy.url.withoutTrailingSlash

/**
 * Checks whether the websiteIRI can be found in storage.
 */
internal suspend fun ApplicationRequest.manifestExists(websiteIRI: String): Boolean =
    call.application.persistentStorage.hexists(
        LookupKeys.Manifest.name,
        hashKey = Url(websiteIRI).withoutTrailingSlash,
    )

/**
 * Retrieves the manifest value from storage.
 */
internal suspend fun Application.getManifestOrNull(websiteIRI: String): Manifest? {
    val manifest = persistentStorage.getHashValue(
        LookupKeys.Manifest.name,
        hashKey = Url(websiteIRI).withoutTrailingSlash,
    ) ?: return null

    return libroConfig.serializer.decodeFromString(manifest)
}
