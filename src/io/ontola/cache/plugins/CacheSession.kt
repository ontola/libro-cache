package io.ontola.cache.plugins

import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
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
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.sessions.OIDCRequest
import io.ontola.cache.sessions.OIDCTokenResponse
import io.ontola.cache.sessions.SessionData
import io.ontola.cache.util.copy

class SessionManager(private val call: ApplicationCall, private val configuration: CacheSession.Configuration) {
    var session: SessionData?
        get() = call.sessions.get<SessionData>()
        set(value) = call.sessions.set(value)

    val host: String?
        get() = call.request.header("Host")

    val language: String
        get() = call.request.header("Accept-Language")
//            ?: claimsFromJWT()?.user?.language
            ?: configuration.cacheConfig.defaultLanguage

    suspend fun ensure() {
        if (session != null) {
            return
        }

        val guestToken = guestToken()
        session = SessionData(guestToken.accessToken, guestToken.refreshToken)
    }

    fun setAuthorization(accessToken: String, refreshToken: String) {
        session = SessionData(accessToken = accessToken, refreshToken = refreshToken)
    }

    private suspend fun guestToken(): OIDCTokenResponse {
        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }

        val serviceToken = configuration.serviceToken()
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

class CacheSession(private val configuration: Configuration) {
    class Configuration {
        lateinit var sessionSecret: String
        lateinit var jwtValidator: JWTVerifier
        lateinit var cacheConfig: CacheConfig
        lateinit var oidcClientId: String
        lateinit var oidcClientSecret: String
        lateinit var oAuthToken: String
        lateinit var oidcUrl: String

        suspend fun accessToken(): String? {
            return null
        }

        suspend fun serviceToken(): String? {
            return null
        }
    }

    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        context.call.attributes.put(CacheSessionKey, SessionManager(context.call, configuration))
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, CacheSession> {
        override val key = AttributeKey<CacheSession>("CacheSession")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): CacheSession {
            val cacheConfig = pipeline.cacheConfig
            val configuration = Configuration()
                .apply {
                    this.sessionSecret = cacheConfig.sessions.sessionSecret
                    this.cacheConfig = cacheConfig

//                    this.jwtValidator = cacheConfig.sessions.jwtValidator
                    this.oidcClientId = cacheConfig.sessions.clientId
                    this.oidcClientSecret = cacheConfig.sessions.clientSecret
                    this.oAuthToken = cacheConfig.sessions.oAuthToken
                    this.oidcUrl = cacheConfig.sessions.oidcUrl
                }
                .apply(configure)
            val feature = CacheSession(configuration)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                feature.intercept(this)
            }

            return feature
        }
    }
}

private val CacheSessionKey = AttributeKey<SessionManager>("CacheSessionKey")

internal val ApplicationCall.sessionManager: SessionManager
    get() = attributes.getOrNull(CacheSessionKey) ?: reportMissingTenantization()

private fun ApplicationCall.reportMissingTenantization(): Nothing {
    application.feature(CacheSession) // ensure the feature is installed
    throw CacheSessionNotYetConfiguredException()
}
class CacheSessionNotYetConfiguredException :
    IllegalStateException("Cache sessions are not yet ready: you are asking it to early before the CacheSessions feature.")
