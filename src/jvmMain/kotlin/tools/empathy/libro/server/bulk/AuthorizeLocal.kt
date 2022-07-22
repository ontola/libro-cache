package tools.empathy.libro.server.bulk

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import tools.empathy.libro.server.landing.landingPage
import tools.empathy.vocabularies.LibroData
import tools.empathy.vocabularies.OntolaData
import tools.empathy.vocabularies.SchemaData

suspend fun ApplicationCall.authorizeLocal(_toAuthorize: Flow<CacheRequest>): Flow<CacheEntry> {
    val data = landingPage() + SchemaData + LibroData + OntolaData

    return data.values.map { record ->
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
