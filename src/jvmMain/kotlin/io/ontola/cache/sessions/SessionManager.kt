package io.ontola.cache.sessions

import com.auth0.jwt.exceptions.TokenExpiredException
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ontola.cache.plugins.CacheSessionConfiguration
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.deviceId
import io.ontola.cache.plugins.logger
import io.ontola.cache.plugins.setPreferredLanguage
import io.ontola.cache.tenantization.tenant
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.copy
import io.ontola.cache.util.proxySafeHeaders
import io.ontola.util.appendPath
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

val unsafeMethods = listOf(
    HttpMethod.Post,
    HttpMethod.Put,
    HttpMethod.Patch,
    HttpMethod.Delete,
)

class SessionManager(
    private val call: ApplicationCall,
    private val configuration: CacheSessionConfiguration,
    private val refresher: SessionRefresher = SessionRefresher(configuration),
) {
    var session: SessionData?
        get() = refreshIfExpired(call.sessions.get<SessionData>())
        set(value) {
            if (value == null) {
                call.sessions.clear<SessionData>()
            } else {
                call.sessions.set(value)
            }
            claims()?.user?.language.let {
                call.setPreferredLanguage(it)
            }
        }

    val host: String?
        get() = call.request.header(HttpHeaders.Host)

    val language: String?
        get() = claims()?.user?.language

    val isUser: Boolean
        get() = claims()?.user?.type == UserType.User

    val isStaff: Boolean
        get() = claims()?.scopes?.contains("staff") ?: false

    val logoutRequest: LogoutRequest?
        get() = session?.credentials?.accessToken?.let {
            LogoutRequest(
                configuration.oidcClientId,
                configuration.oidcClientSecret,
                it,
            )
        }

    private fun refreshIfExpired(session: SessionData?): SessionData? {
        return try {
            session?.claims(configuration.jwtValidator)

            session
        } catch (e: TokenExpiredException) {
            runBlocking {
                this@SessionManager.session = refresher.refresh(session!!)
                this@SessionManager.session
            }.also { it?.claims(configuration.jwtValidator) }
        }
    }

    private fun requireAccessToken(): Boolean {
        return unsafeMethods.contains(call.request.httpMethod)
    }

    private fun claims(): Claims? {
        return session?.let {
            try {
                it.claims(configuration.jwtValidator)
            } catch (e: TokenExpiredException) {
                session = runBlocking {
                    refresher.refresh(it)
                }
                it.claims(configuration.jwtValidator)
            }
        }
    }

    suspend fun ensure() {
        val existing = session ?: SessionData()

        if (existing.credentials == null) {
            if (requireAccessToken()) {
                val guestToken = guestToken()
                session = existing.copy(
                    credentials = TokenPair(
                        guestToken.accessToken,
                        guestToken.refreshToken,
                    ),
                    deviceId = call.deviceId,
                )
            }
        } else if (existing.isExpired(configuration.jwtValidator)) {
            session = refresher.refresh(existing)
        }
    }

    fun ensureCsrf() {
        val existing = session

        if (existing == null) {
            session = SessionData()
        }
    }

    fun delete() {
        session = null
    }

    fun setAuthorization(accessToken: String, refreshToken: String) {
        session = (session ?: SessionData()).copy(
            credentials = TokenPair(
                accessToken = accessToken,
                refreshToken = refreshToken,
            ),
            deviceId = call.deviceId,
        )
    }

    @Deprecated("Until sessions are migrated")
    private suspend fun legacyOrFresh(): SessionData? {
        return getLegacySessionOrNull(call, configuration)?.let {
            if (it.userToken == null || it.refreshToken == null) return null

            SessionData(
                credentials = TokenPair(
                    accessToken = it.userToken,
                    refreshToken = it.refreshToken,
                ),
                deviceId = call.deviceId,
            )
        }
    }

    private suspend fun guestToken(): OIDCTokenResponse {
        val serviceToken = configuration.oAuthToken
        val path = call.tenant.websiteIRI.appendPath("oauth", "token").fullPath
        val response = call.application.cacheConfig.client.post(configuration.oidcUrl.appendPath(path)) {
            expectSuccess = false

            headers {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $serviceToken")

                header(CacheHttpHeaders.WebsiteIri, call.tenant.websiteIRI)

                proxySafeHeaders(call.request)
                header(CacheHttpHeaders.XDeviceId, call.deviceId)
                copy("X-Real-Ip", call.request)
                copy("X-Requested-With", call.request)
            }

            setBody(
                OIDCRequest.guestRequest(
                    configuration.oidcClientId,
                    configuration.oidcClientSecret,
                )
            )
        }

        if (response.status == HttpStatusCode.BadRequest) {
            val error = Json.decodeFromString<BackendErrorResponse>(response.body())
            call.logger.warn { "E: ${error.error} - ${error.code} - ${error.errorDescription}" }
            if (error.error == "invalid_grant") {
                throw InvalidGrantException()
            } else {
                throw RuntimeException("Unexpected body with status 400")
            }
        }

        return response.body()
    }
}
