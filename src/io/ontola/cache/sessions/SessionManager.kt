package io.ontola.cache.sessions

import io.ktor.application.ApplicationCall
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.fullPath
import io.ktor.request.header
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ontola.cache.plugins.CacheSession
import io.ontola.cache.plugins.tenant
import io.ontola.cache.util.copy

class SessionManager(
    private val call: ApplicationCall,
    private val configuration: CacheSession.Configuration,
    private val refresher: SessionRefresher = SessionRefresher(configuration),
) {
    var session: SessionData?
        get() = call.sessions.get<SessionData>()
        set(value) = call.sessions.set(value)

    val host: String?
        get() = call.request.header("Host")

    val language: String
        get() = call.request.header("Accept-Language")
            ?: session?.claims(configuration.jwtValidator)?.user?.language
            ?: configuration.cacheConfig.defaultLanguage

    suspend fun ensure() {
        val existing = session

        if (existing == null) {
            val guestToken = guestToken()
            session = SessionData(guestToken.accessToken, guestToken.refreshToken)
        } else if (existing.isExpired(configuration.jwtValidator)) {
            session = refresher.refresh(existing)
        }
    }

    fun setAuthorization(accessToken: String, refreshToken: String) {
        session = SessionData(accessToken = accessToken, refreshToken = refreshToken)
    }

    @Deprecated("Until sessions are migrated")
    private suspend fun legacyOrFresh(): SessionData? {
        return getLegacySessionOrNull(call, configuration)?.let {
            if (it.userToken == null || it.refreshToken == null) return null

            SessionData(
                accessToken = it.userToken,
                refreshToken = it.refreshToken,
            )
        }
    }

    private suspend fun guestToken(): OIDCTokenResponse {
        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }

        val serviceToken = configuration.oAuthToken
        val path = "${call.tenant.websiteIRI.fullPath}/oauth/token"

        return client.request("${configuration.oidcUrl}$path") {
            method = HttpMethod.Post

            headers {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $serviceToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)

                copy("Accept-Language", call.request)
                header("X-Forwarded-Host", call.request.header("Host"))

                copy("X-Forwarded-Host", call.request)
                copy("X-Forwarded-Proto", call.request)
                copy("X-Forwarded-Ssl", call.request)
                copy("X-Real-Ip", call.request)
                copy("X-Requested-With", call.request)
                copy("X-Request-Id", call.request)
            }

            headers {
                header("X-Argu-Back", "true")
                header("X-Device-Id", "") // TODO
                header("Website-IRI", call.tenant.websiteIRI)
            }

            body = OIDCRequest.guestRequest(configuration.oidcClientId, configuration.oidcClientSecret)
        }
    }
}
