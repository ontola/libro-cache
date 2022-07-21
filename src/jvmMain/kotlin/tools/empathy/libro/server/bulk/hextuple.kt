package tools.empathy.libro.server.bulk

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

fun statusCode(iri: String, status: HttpStatusCode): String = hextuple(
    iri,
    "http://www.w3.org/2011/http#statusCode",
    status.value.toString(10),
    "http://www.w3.org/2001/XMLSchema#integer",
    "",
    "http://purl.org/link-lib/meta",
)

fun isA(iri: String, value: String): String = hextuple(
    iri,
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
    value,
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode",
    "",
    "rdf:defaultGraph",
)

fun hextuple(
    subject: String,
    predicate: String,
    value: String,
    datatype: String,
    language: String = "",
    graph: String,
): String {
    val statement = buildJsonArray {
        add(subject)
        add(predicate)
        add(value)
        add(datatype)
        add(language)
        add(graph)
    }

    return statement.toString()
}
