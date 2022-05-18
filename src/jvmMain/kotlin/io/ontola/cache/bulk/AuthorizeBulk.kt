package io.ontola.cache.bulk

import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
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
import io.ontola.cache.initializeOpenTelemetry
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.deviceId
import io.ontola.cache.plugins.language
import io.ontola.cache.plugins.services
import io.ontola.cache.sessions.SessionData
import io.ontola.cache.sessions.TokenPair
import io.ontola.cache.tenantization.tenant
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.measured
import io.ontola.util.appendPath
import io.opentelemetry.context.Context
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.css.header
import mu.KotlinLogging
import kotlin.time.Duration.Companion.seconds

val logger = KotlinLogging.logger {}

private suspend fun ApplicationCall.executeBulkAuthorize(resources: List<String>): HttpResponse = measured("executeBulkAuthorize") { span ->
    val bulkUri = URLBuilder(tenant.websiteIRI)
        .apply { appendPath("spi", "bulk") }
        .build()
        .encodedPath

    span.setAttribute(SemanticAttributes.HTTP_METHOD, "POST")
    span.setAttribute(SemanticAttributes.HTTP_URL, bulkUri)

    application.cacheConfig.client.post(services.route(bulkUri)) {
        timeout {
            requestTimeoutMillis = 120.seconds.inWholeMilliseconds
        }
        contentType(ContentType.Application.Json)
        initHeaders(this@executeBulkAuthorize, language)
        initializeOpenTelemetry().propagators.textMapPropagator.inject(Context.current(), request) { _, key, value ->
            header(key, value)
            if (key == "traceparent") {
                header("trace-parent", value)
            }
        }
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
}

internal suspend fun ApplicationCall.authorizeBulk(
    resources: List<String>,
): Flow<SPIResourceResponseItem> = measured("authorizeBulk;i=${resources.size}") {
    val res: HttpResponse = executeBulkAuthorize(resources)

    if (res.status != HttpStatusCode.OK) {
        val msg = "Unexpected bulk status ${res.status.value}, location: ${res.headers["Location"]}"
        logger.error(msg)

        throw RuntimeException(msg)
    }

    res.headers[CacheHttpHeaders.XAPIVersion]?.let {
        response.header(CacheHttpHeaders.XAPIVersion, it)
    }

    val newAuthorization = res.headers[CacheHttpHeaders.NewAuthorization]
    val newRefreshToken = res.headers[CacheHttpHeaders.NewRefreshToken]

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
