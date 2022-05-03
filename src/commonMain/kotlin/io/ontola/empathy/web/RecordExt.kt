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

/**  The alternate language versions to generate */
fun Record.alternates(): List<Value>? = fields["_ids"]

/** The canonical url for a localised version */
fun Record.canonical(): List<Value>? = fields["_canonical"]

fun Record.language(): Value? = fields["_language"]?.first()

fun Record.translations(): List<Value>? = fields["_translations"]

fun Record.translation(lang: String): Value = translations()!!.first { it is Value.LangString && it.lang == lang }
