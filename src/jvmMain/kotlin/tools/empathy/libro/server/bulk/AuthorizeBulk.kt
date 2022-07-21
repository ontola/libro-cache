package tools.empathy.libro.server.bulk

import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import mu.KotlinLogging
import tools.empathy.libro.server.plugins.cacheConfig
import tools.empathy.libro.server.plugins.deviceId
import tools.empathy.libro.server.plugins.language
import tools.empathy.libro.server.plugins.services
import tools.empathy.libro.server.sessions.SessionData
import tools.empathy.libro.server.sessions.TokenPair
import tools.empathy.libro.server.tenantization.tenant
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.libro.server.util.measured
import tools.empathy.url.appendPath
import kotlin.time.Duration.Companion.seconds

val logger = KotlinLogging.logger {}

internal suspend fun ApplicationCall.authorizeBulk(
    resources: List<String>,
): Flow<SPIResourceResponseItem> = measured("authorizeBulk;i=${resources.size}") {
    val bulkUri = URLBuilder(tenant.websiteIRI)
        .apply { appendPath("spi", "bulk") }
        .build()
        .encodedPath

    val res: HttpResponse = application.cacheConfig.client.post(services.route(bulkUri)) {
        timeout {
            requestTimeoutMillis = 120.seconds.inWholeMilliseconds
        }
        contentType(ContentType.Application.Json)
        initHeaders(this@authorizeBulk, language)
        setBody(
            SPIAuthorizeRequest(
                resources = resources.map { r ->
                    SPIResourceRequestItem(
                        iri = r,
                        include = true,
                    )
                }
            )
        )
    }

    if (res.status != HttpStatusCode.OK) {
        val msg = "Unexpected bulk status ${res.status.value}, location: ${res.headers["Location"]}"
        logger.error(msg)

        throw RuntimeException(msg)
    }

    res.headers[LibroHttpHeaders.XAPIVersion]?.let {
        response.header(LibroHttpHeaders.XAPIVersion, it)
    }

    val newAuthorization = res.headers[LibroHttpHeaders.NewAuthorization]
    val newRefreshToken = res.headers[LibroHttpHeaders.NewRefreshToken]

    if (newAuthorization != null && newRefreshToken != null) {
        val existing = sessions.get<SessionData>() ?: SessionData()
        val newSession = existing.copy(
            credentials = TokenPair(
                accessToken = newAuthorization,
                refreshToken = newRefreshToken,
            ),
            deviceId = deviceId,
        )

        sessions.set(newSession)
    }

    res.body<List<SPIResourceResponseItem>>().asFlow()
}
