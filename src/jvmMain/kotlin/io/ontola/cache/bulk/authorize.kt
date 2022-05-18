package io.ontola.cache.bulk

import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.server.application.ApplicationCall
import io.ontola.cache.plugins.services
import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.merge
import io.opentelemetry.extension.annotations.WithSpan
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

@OptIn(FlowPreview::class)
@WithSpan
suspend fun ApplicationCall.authorize(toAuthorize: Flow<CacheRequest>): Flow<CacheEntry> {
    return toAuthorize
        .toList()
        .groupBy { services.resolve(Url(it.iri).fullPath) }
        .map {
            val service = it.key
            val resources = it.value.map { e -> e.iri }

            if (service.bulk) {
                authorizeBulk(resources)
            } else {
                authorizePlain(resources)
            }
        }
        .asFlow()
        .flattenConcat()
        .map { entry ->
            CacheEntry(
                iri = entry.iri,
                status = HttpStatusCode.fromValue(entry.status),
                cacheControl = entry.cache,
                contents = scopeBlankNodes(entry.body)
                    ?.split("\n")
                    ?.map { Json.decodeFromString<DataSlice>(it) }
                    ?.merge(),
            )
        }
}

fun scopeBlankNodes(hex: String?): String? {
    if (hex == null) {
        return null
    }
    val unique = UUID.randomUUID().toString()

    return hex.replace("\"_:", "\"_:$unique")
}
