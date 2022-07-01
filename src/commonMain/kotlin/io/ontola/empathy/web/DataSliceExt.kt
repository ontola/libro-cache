package io.ontola.empathy.web

import io.ktor.http.Url
import io.ontola.util.absolutize
import io.ontola.util.appendPath
import io.ontola.util.rebase

/**
 * Merges the slices in this list into one slice.
 * Duplicate [Record]s are replaced by later entries.
 */
fun List<DataSlice>.merge(): DataSlice = buildMap {
    for (record in this@merge) {
        for ((k, v) in record) {
            put(k, v)
        }
    }
}

/**
 * Absolutises every global id in this slice to [websiteIRI].
 */
fun DataSlice.compact(websiteIRI: Url?): DataSlice {
    websiteIRI ?: return this

    return this.map { (key, value) ->
        val id = if (key.startsWith("_"))
            Value.Id.Local(key)
        else
            shortenedGlobalId(key, websiteIRI)

        id.value to value.compact(websiteIRI)
    }.toMap()
}

fun DataSlice.splitMultilingual(websiteIRI: Url): DataSlice {
    val localisedRecords = mutableMapOf<Value, Record>()

    return this
        .entries
        .flatMap { (_, v) ->
            val ids = v.alternates()
            if (ids.isNullOrEmpty()) {
                return@flatMap listOf(v)
            }

            val translations = mutableListOf<Value.LangString>()
            val localised = ids.filterIsInstance<Value.LangString>().map { slug ->
                val id = when (v.id) {
                    is Value.Id.Global -> v.id.localised(websiteIRI, slug.lang, slug.value)
                    is Value.Id.Local -> v.id.localised(slug.lang)
                    else -> throw Error("Primitive as id")
                }
                translations.add(Value.LangString(id.toString(), slug.lang))

                Record(
                    id = id,
                    fields = v.fields.filterLanguage(slug.lang, this)
                ).also {
                    it.fields["_canonical"] = listOf(v.id)
                    it.fields["_language"] = listOf(Value.Str(slug.lang))
                }
            }
            val canonical = Record(
                id = v.id,
                fields = mutableMapOf(
                    "_translations" to translations,
                )
            )

            localisedRecords[canonical.id] = canonical

            buildList {
                addAll(localised)
                add(canonical)
            }
        }
        .map { record ->
            val lang = record.language()?.value ?: return@map record

            record.copy(
                fields = record.fields.mapValues { (field, values) ->
                    if (field == "_canonical") return@mapValues values

                    values.map { value ->
                        if (localisedRecords.contains(value)) {
                            localisedRecords[value]!!.translation(lang).let { Value.Id.Global(it.value) }
                        } else {
                            value
                        }
                    }
                }.toMutableMap()
            )
        }
        .associateBy { r -> r.id.value }
}

fun Value.Id.Global.localised(websiteIRI: Url, language: String, segmentName: String? = null): Url {
    val recordPath = websiteIRI.absolutize(value).let {
        val parts = it.split('/')
        if (segmentName == null || parts.isEmpty())
            return@let it

        parts.toMutableList().apply {
            set(parts.size - 1, segmentName)
        }.joinToString("/")
    }
    return websiteIRI.appendPath(language).rebase(recordPath)
}

fun Value.Id.Local.localised(language: String): Url {
    return Url("$id-$language")
}

fun FieldSet.filterLanguage(lang: String, slice: DataSlice): FieldSet = mapValues { (_, v) ->
    v.filter {
        if (it is Value.Id.Global && slice.isTranslatedObject(it)) {
            slice[it.value]?.get("https://ns.ontola.io/libro/language")?.first()?.value == lang
        } else if (it !is Value.LangString) {
            true
        } else {
            it.lang == lang
        }
    }.map {
        if (it is Value.Id.Global && slice.isTranslatedObject(it)) {
            slice[it.value]?.get("https://ns.ontola.io/libro/value")?.first() ?: it
        } else {
            it
        }
    }
}.toMutableMap()

fun DataSlice?.isTranslatedObject(id: Value.Id.Global): Boolean = this
    ?.get(id.value)
    ?.get("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
    ?.contains(Value.Id.Global("https://ns.ontola.io/libro/TranslatedObject"))
    ?: false

fun DataSlice.normaliseAbsolutePaths(): DataSlice {
    fun normaliseId(id: String) = if (id.startsWith("_"))
        Value.Id.Local(id)
    else if (id.startsWith("#")) {
        Value.Id.Global("/$id")
    } else
        Value.Id.Global(id)

    return this.map { (key, value) ->
        val id = normaliseId(key)
        val fields = value.fields.mapValues { (_, values) ->
            values.map { v ->
                if (v is Value.Id.Global)
                    normaliseId(v.value)
                else
                    v
            }
        }.toMutableMap()

        id.value to value.copy(
            id = id,
            fields = fields,
        )
    }.toMap()
}
