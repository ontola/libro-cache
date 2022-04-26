package io.ontola.cache.bulk

import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.server.application.ApplicationCall
import io.ontola.cache.plugins.services
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.util.UUID

@OptIn(FlowPreview::class)
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
        .map {
            CacheEntry(
                iri = it.iri,
                status = HttpStatusCode.fromValue(it.status),
                cacheControl = it.cache,
                contents = scopeBlankNodes(it.body),
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
