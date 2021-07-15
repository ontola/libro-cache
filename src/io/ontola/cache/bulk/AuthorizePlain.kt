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
import io.ontola.cache.features.cacheConfig
import io.ontola.cache.features.services
import io.ontola.cache.features.session
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

internal suspend fun PipelineContext<Unit, ApplicationCall>.authorizePlain(
    resources: List<String>,
): List<SPIResourceResponseItem> {
    val lang = call.session.language()

    return resources
        .asFlow()
        .map {
            it to call.application.cacheConfig.client.get<HttpResponse> {
                url(call.services.route(Url(it).fullPath))
                initHeaders(call, lang)
                headers {
                    header("Accept", "application/hex+x-ndjson")
                }
                expectSuccess = false
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
