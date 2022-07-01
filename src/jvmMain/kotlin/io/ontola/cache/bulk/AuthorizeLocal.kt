package io.ontola.cache.bulk

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import tools.empathy.libro.landingPage

suspend fun ApplicationCall.authorizeLocal(_toAuthorize: Flow<CacheRequest>): Flow<CacheEntry> {
    val staticRecords = landingPage()

    return staticRecords.values.map { record ->
        CacheEntry(
            record.id.value,
            HttpStatusCode.OK,
            CacheControl.Private,
            mapOf(
                record.id.value to record,
            ),
        )
    }.asFlow()
}
