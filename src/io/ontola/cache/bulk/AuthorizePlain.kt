package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.client.call.receive
import io.ktor.client.features.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.util.pipeline.PipelineContext
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.services
import io.ontola.cache.plugins.session
import io.ontola.cache.util.measured
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

internal suspend fun PipelineContext<Unit, ApplicationCall>.authorizePlain(
    resources: List<String>,
): List<SPIResourceResponseItem> = measured("authorizePlain;i=${resources.size}") {
    val lang = call.session.language()

    resources
        .asFlow()
        .map {
            it to measured("authorizePlain - $it") {
                call.application.cacheConfig.client.get<HttpResponse> {
                    url(call.services.route(Url(it).fullPath))
                    initHeaders(call, lang)
                    headers {
                        header("Accept", "application/hex+x-ndjson")
                    }
                    expectSuccess = false
                }
            }
        }.map { (iri, response) ->
            SPIResourceResponseItem(
                iri = iri,
                status = response.status.value,
                cache = CacheControl.Private,
                language = lang,
                body = response.receive()
            )
        }.toList()
}
