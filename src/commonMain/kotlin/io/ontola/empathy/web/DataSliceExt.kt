package io.ontola.empathy.web

import io.ktor.http.Url

fun List<DataSlice>.merge(): DataSlice = buildMap {
    for (record in this@merge) {
        for ((k, v) in record) {
            put(k, v)
        }
    }
}

fun DataSlice.compact(websiteIRI: Url?): DataSlice {
    websiteIRI ?: return this

    return this.map { (key, value) ->
        val id = if (key.startsWith("_"))
            Value.LocalId(key)
        else
            shortenedGlobalId(key, websiteIRI)

        id.value to value.compact(websiteIRI)
    }.toMap()
}
