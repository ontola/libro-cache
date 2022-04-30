package io.ontola.empathy.web

import io.ktor.http.Url

fun Record.compact(websiteIRI: Url?): Record {
    websiteIRI ?: return this

    fun shortenId(id: String): Value = if (id.startsWith("_"))
        Value.LocalId(id)
    else
        shortenedGlobalId(id, websiteIRI)

    val compactedFields = fields
        .mapKeys { (key) -> shortenId(key).value }
        .mapValues { (k, value) ->
            value.map {
                if (it is Value.GlobalId) {
                    shortenedGlobalId(it.value, websiteIRI)
                } else {
                    it
                }
            }
        }
        .toMutableMap()

    return Record(shortenId(id.value), compactedFields)
}
