package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.call.receive
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.deviceId
import io.ontola.cache.plugins.services
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.sessions.SessionData
import io.ontola.cache.tenantization.tenant
import io.ontola.cache.util.CacheHttpHeaders
import io.ontola.cache.util.measured
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

internal suspend fun PipelineContext<Unit, ApplicationCall>.authorizeBulk(
    resources: List<String>,
): Flow<SPIResourceResponseItem> = measured("authorizeBulk;i=${resources.size}") {
    val lang = call.sessionManager.language
    val prefix = call.tenant.websiteIRI.encodedPath.split("/").getOrNull(1)?.let { "/$it" } ?: ""

    val res: HttpResponse = call.application.cacheConfig.client.post {
        url(call.services.route("$prefix/spi/bulk"))
        contentType(ContentType.Application.Json)
        initHeaders(call, lang)
        headers {
            header("Accept", ContentType.Application.Json)
            header("Content-Type", ContentType.Application.Json)
        }
        body = SPIAuthorizeRequest(
            resources = resources.map { r ->
                SPIResourceRequestItem(
                    iri = r,
                    include = true,
                )
            }
        )
    }

    if (res.status != HttpStatusCode.OK) {
        val msg = "Unexpected bulk status ${res.status.value}, location: ${res.headers["Location"]}"
        logger.error(msg)

        throw RuntimeException(msg)
    }

    val newAuthorization = res.headers[CacheHttpHeaders.NewAuthorization]
    val newRefreshToken = res.headers[CacheHttpHeaders.NewRefreshToken]

    if (newAuthorization != null && newRefreshToken != null) {
        val newSession = SessionData(
            accessToken = newAuthorization,
            refreshToken = newRefreshToken,
            deviceId = call.deviceId,
        )

        call.sessions.set(newSession)
    }

    res.receive<List<SPIResourceResponseItem>>().asFlow()
}
