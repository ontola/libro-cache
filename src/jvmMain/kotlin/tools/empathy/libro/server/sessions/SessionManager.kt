package tools.empathy.libro.server.sessions

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
import io.ktor.server.sessions.sessionId
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.plugins.CacheSessionConfiguration
import tools.empathy.libro.server.plugins.deviceId
import tools.empathy.libro.server.plugins.setPreferredLanguage
import tools.empathy.libro.server.sessions.oidc.OIDCServerSettings
import tools.empathy.libro.server.tenantization.tenant
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.libro.server.util.copy
import tools.empathy.libro.server.util.proxySafeHeaders
import tools.empathy.url.appendPath

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
    val logger = KotlinLogging.logger {}

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

    private val oidcSettings: OIDCServerSettings
        get() = runBlocking { configuration.oidcSettingsManager.getOrCreate()!! }

    val logoutRequest: LogoutRequest?
        get() = session?.credentials?.accessToken?.let {
            LogoutRequest(
                oidcSettings.credentials.clientId,
                oidcSettings.credentials.clientSecret,
                it,
            )
        }

    private fun refreshIfExpired(session: SessionData?): SessionData? {
        return try {
            session?.claims(configuration.jwtValidator)

            session
        } catch (e: TokenExpiredException) {
            logger.trace { "Refreshing expired token." }
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

    suspend fun ensure(retry: Boolean = true) {
        try {
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
        } catch (e: SessionException) {
            if (retry && e is InvalidClientException) {
                configuration.oidcSettingsManager.refresh()
                return ensure(false)
            }

            throw e
        }
    }

    fun ensureCsrf() {
        val existing = session

        if (existing == null) {
            session = SessionData()
        }
    }

    fun delete() {
        logger.trace { "Deleting session ${call.sessionId}" }
        session = null
    }

    fun setAuthorization(accessToken: String, refreshToken: String) {
        if (configuration.libroConfig.isDev) {
            logger.debug {
                val prefix = session?.let { "Updating session ${call.sessionId}" } ?: "Creating session"
                "$prefix with access '$accessToken' and refresh '$refreshToken'"
            }
        }

        session = (session ?: SessionData()).copy(
            credentials = TokenPair(
                accessToken = accessToken,
                refreshToken = refreshToken,
            ),
            deviceId = call.deviceId,
        )
    }

    private suspend fun guestToken(): OIDCTokenResponse {
        logger.trace { "Requesting guest token" }
        val path = call.tenant.websiteIRI.appendPath("oauth", "token").fullPath
        val response = call.application.libroConfig.client.post(configuration.oidcUrl.appendPath(path)) {
            expectSuccess = false

            headers {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)

                header(LibroHttpHeaders.WebsiteIri, call.tenant.websiteIRI)

                proxySafeHeaders(call.request)
                header(LibroHttpHeaders.XDeviceId, call.deviceId)
                copy("X-Real-Ip", call.request)
                copy("X-Requested-With", call.request)
            }

            setBody(
                OIDCRequest.guestRequest(
                    oidcSettings.credentials.clientId,
                    oidcSettings.credentials.clientSecret,
                ),
            )
        }

        if (response.status.value >= HttpStatusCode.BadRequest.value) {
            throwSessionException(response)
        }

        return response.body()
    }
}
