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
import io.ontola.cache.plugins.deviceId
import io.ontola.cache.plugins.tenant
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.copy
import io.ontola.cache.util.proxySafeHeaders

class SessionManager(
    private val call: ApplicationCall,
    private val configuration: CacheSession.Configuration,
    private val refresher: SessionRefresher = SessionRefresher(configuration),
) {
    var session: SessionData?
        get() = call.sessions.get<SessionData>()
        set(value) = call.sessions.set(value)

    val host: String?
        get() = call.request.header(HttpHeaders.Host)

    val language: String
        get() = call.request.header(HttpHeaders.AcceptLanguage)
            ?: session?.claims(configuration.jwtValidator)?.user?.language
            ?: configuration.cacheConfig.defaultLanguage

    val loggedIn: Boolean
        get() = session?.claims(configuration.jwtValidator)?.user?.type == UserType.User

    val logoutRequest: LogoutRequest?
        get() = session?.accessToken?.let {
            LogoutRequest(
                configuration.oidcClientId,
                configuration.oidcClientSecret,
                it,
            )
        }

    suspend fun ensure() {
        val existing = session

        if (existing == null) {
            val guestToken = guestToken()
            session = SessionData(
                guestToken.accessToken,
                guestToken.refreshToken,
                call.deviceId,
            )
        } else if (existing.isExpired(configuration.jwtValidator)) {
            session = refresher.refresh(existing)
        }
    }

    fun delete() {
        session = null
    }

    fun setAuthorization(accessToken: String, refreshToken: String) {
        session = SessionData(
            accessToken = accessToken,
            refreshToken = refreshToken,
            deviceId = call.deviceId,
        )
    }

    @Deprecated("Until sessions are migrated")
    private suspend fun legacyOrFresh(): SessionData? {
        return getLegacySessionOrNull(call, configuration)?.let {
            if (it.userToken == null || it.refreshToken == null) return null

            SessionData(
                accessToken = it.userToken,
                refreshToken = it.refreshToken,
                deviceId = call.deviceId,
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

                header(CacheHttpHeaders.WebsiteIri, call.tenant.websiteIRI)

                proxySafeHeaders(call.request)
                header(CacheHttpHeaders.XDeviceId, call.deviceId)
                copy("X-Real-Ip", call.request)
                copy("X-Requested-With", call.request)
            }

            body = OIDCRequest.guestRequest(configuration.oidcClientId, configuration.oidcClientSecret)
        }
    }
}
