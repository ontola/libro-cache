package tools.empathy.libro.server.bulk

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import tools.empathy.libro.server.tenantization.TenantData

suspend fun ApplicationCall.authorizeLocal(tenant: TenantData.Local, _toAuthorize: Flow<CacheRequest>): Flow<CacheEntry> {
    val context = tenant.context.invoke(this)

    return context.data?.values?.map { record ->
        CacheEntry(
            record.id.value,
            HttpStatusCode.OK,
            CacheControl.Private,
            mapOf(
                record.id.value to record,
            ),
        )
    }?.asFlow() ?: emptyFlow()
}
