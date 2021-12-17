package io.ontola.cache.bulk

import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.services
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.util.measured
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

internal suspend fun PipelineContext<Unit, ApplicationCall>.authorizePlain(
    resources: List<String>,
): Flow<SPIResourceResponseItem> = measured("authorizePlain;i=${resources.size}") {
    val lang = call.sessionManager.language

    resources
        .asFlow()
        .map {
            it to measured("authorizePlain - $it") {
                call.application.cacheConfig.client.get {
                    url(call.services.route(Url(it).fullPath))
                    initHeaders(call, lang)
                    headers {
                        header("Accept", "application/hex+x-ndjson")
                    }
                    expectSuccess = false
                }
            }
        }
        .map { (iri, response) ->
            SPIResourceResponseItem(
                iri = iri,
                status = response.status.value,
                cache = CacheControl.Private,
                language = lang,
                body = response.body()
            )
        }
}
