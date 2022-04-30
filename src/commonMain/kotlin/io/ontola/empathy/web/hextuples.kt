package io.ontola.empathy.web

import io.ktor.http.Url
import io.ontola.rdf.hextuples.DataType
import io.ontola.rdf.hextuples.Hextuple

const val supplantGraph = "http://purl.org/linked-delta/supplant"

fun Hextuple.toValue(websiteIRI: Url?): Value = when (datatype) {
    is DataType.GlobalId -> shortenedGlobalId(value, websiteIRI)
    is DataType.LocalId -> Value.LocalId("_:$value")
    else -> when (datatype.value()) {
        "http://www.w3.org/2001/XMLSchema#string" -> Value.Str(value)
        "http://www.w3.org/2001/XMLSchema#boolean" -> Value.Bool(value)
        "http://www.w3.org/2001/XMLSchema#integer" -> Value.Int(value)
        "http://www.w3.org/2001/XMLSchema#long" -> Value.Long(value)
        "http://www.w3.org/2001/XMLSchema#dateTime" -> Value.DateTime(value)
        else -> if (language == "")
            Value.Primitive(value, datatype.value())
        else
            Value.LangString(value, language)
    }
}

fun List<Hextuple?>.toSlice(websiteIRI: Url? = null): DataSlice = buildMap {
    for (hex in this@toSlice) {
        if (hex == null || hex.graph == "http://purl.org/link-lib/meta") continue

        if (hex.graph != supplantGraph) throw Error("Non-supplant statement: $hex")

        val id = if (hex.subject.startsWith("_"))
            Value.LocalId(hex.subject)
        else
            shortenedGlobalId(hex.subject, websiteIRI)

        val record = this.getOrPut(id.value) {
            Record(id)
        }
        val field = record.entries.getOrPut(shortenedGlobalIdString(hex.predicate, websiteIRI)) { listOf() }
        val value = hex.toValue(websiteIRI)
        record[shortenedGlobalIdString(hex.predicate, websiteIRI)] = listOf(*field.toTypedArray(), value)

        put(id.value, record)
    }
}
