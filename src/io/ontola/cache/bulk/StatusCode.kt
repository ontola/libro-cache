package io.ontola.cache.bulk

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

fun statusCode(iri: String, status: HttpStatusCode): String {
    val statement = buildJsonArray {
        add(iri)
        add("http://www.w3.org/2011/http#statusCode")
        add(status.value.toString(10))
        add("http://www.w3.org/2001/XMLSchema#integer")
        add("")
        add("http://purl.org/link-lib/meta")
    }

    return statement.toString()
}
