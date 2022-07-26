package tools.empathy.libro.server.bulk

import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.plugins.language
import tools.empathy.libro.server.plugins.services
import tools.empathy.libro.server.tenantization.tenant
import tools.empathy.libro.server.util.measured
import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.dataSlice
import tools.empathy.serialization.merge
import tools.empathy.url.retrieveIriParamOrSelf
import java.util.UUID

@OptIn(FlowPreview::class)
suspend fun ApplicationCall.authorizeApex(toAuthorize: Flow<CacheRequest>): Flow<CacheEntry> {
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

suspend fun ApplicationCall.authorizeCors(toAuthorize: Flow<CacheRequest>): Flow<CacheEntry> {
    val lang = language

    return toAuthorize
        .map { Url(it.iri).retrieveIriParamOrSelf().toString() }
        .map {
            it to measured("fetchCors - $it") {
                application.libroConfig.client.get {
                    url(it)
                    initHeaders(this@authorizeCors, lang)
                    headers {
                        header("Accept", "application/empathy+json")
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
        .map { entry ->
            CacheEntry(
                iri = entry.iri,
                status = HttpStatusCode.fromValue(entry.status),
                cacheControl = entry.cache,
                contents = scopeBlankNodes(entry.body)
                    ?.split("\n")
                    ?.map {
                        try {
                            Json.decodeFromString(it)
                        } catch (e: SerializationException) {
                            dataSlice {}
                        }
                    }
                    ?.merge(),
            )
        }
}

suspend fun ApplicationCall.authorize(toAuthorize: Flow<CacheRequest>): Flow<CacheEntry> {
    return if (tenant.websiteIRI == Url("https://localhost")) {
        authorizeLocal(toAuthorize)
    } else if (tenant.isCors) {
        authorizeCors(toAuthorize)
    } else {
        authorizeApex(toAuthorize)
    }
}

fun scopeBlankNodes(hex: String?): String? {
    if (hex == null) {
        return null
    }
    val unique = UUID.randomUUID().toString()

    return hex.replace("\"_:", "\"_:$unique")
}
