package tools.empathy.serialization

import io.ktor.http.Url

/**
 * Compacts the ids of the record and field references based on the [websiteIRI].
 */
fun Record.compact(websiteIRI: Url?): Record {
    websiteIRI ?: return this

    fun shortenId(id: String): Value.Id = if (id.startsWith("_"))
        Value.Id.Local(id)
    else
        shortenedGlobalId(id, websiteIRI)

    val compactedFields = fields
        .mapKeys { (key) -> shortenId(key).value }
        .mapValues { (_, value) ->
            value.map {
                if (it is Value.Id.Global) {
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

fun Record.translations(): List<Value.LangString>? = fields["_translations"]?.filterIsInstance<Value.LangString>()

fun Record.translation(lang: String): Value = translations()!!.first { it.lang == lang }
