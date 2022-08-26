@file:UseSerializers(UrlSerializer::class)

package tools.empathy.libro.server.sessions.oidc

import io.ktor.http.Url
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.plugins.Storage
import tools.empathy.libro.server.util.UrlSerializer

class OIDCSettingsRepository(private val storage: Storage) {
    suspend fun setByOrigin(origin: Url, settings: OIDCServerSettings) {
        storage.setString(
            "oidc",
            "registration",
            origin.toString(),
            value = Json.encodeToString(OIDCServerSettings.serializer(), settings),
            expiration = null,
        )
    }

    suspend fun getByOrigin(origin: Url): OIDCServerSettings? {
        val registration = storage.getString("oidc", "registration", origin.toString()) ?: return null

        return Json.decodeFromString(OIDCServerSettings.serializer(), registration)
    }

    suspend fun deleteByOrigin(origin: Url) {
        storage.deleteKey(
            "oidc",
            "registration",
            origin.toString(),
        )
    }
}
