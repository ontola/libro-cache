package io.ontola.empathy.web

import io.ktor.http.Url
import io.ontola.util.absolutize
import io.ontola.util.appendPath
import io.ontola.util.rebase

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
                val id = (v.id as Value.GlobalId).localised(websiteIRI, slug.lang, slug.value)
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
                fields = record.fields.mapValues { (_, values) ->
                    values.map { value ->
                        if (localisedRecords.contains(value)) {
                            localisedRecords[value]!!.translation(lang).let { Value.GlobalId(it.value) }
                        } else {
                            value
                        }
                    }
                }.toMutableMap()
            )
        }
        .associateBy { r -> r.id.value }
}

fun Value.GlobalId.localised(websiteIRI: Url, language: String, segmentName: String? = null): Url {
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

fun FieldSet.filterLanguage(lang: String, slice: DataSlice): FieldSet = mapValues { (_, v) ->
    v.filter {
        if (it is Value.GlobalId && slice.isTranslatedObject(it)) {
            slice[it.value]?.get("https://ns.ontola.io/libro/language")?.first()?.value == lang
        } else if (it !is Value.LangString) {
            true
        } else {
            it.lang == lang
        }
    }.map {
        if (it is Value.GlobalId && slice.isTranslatedObject(it)) {
            slice[it.value]?.get("https://ns.ontola.io/libro/value")?.first() ?: it
        } else {
            it
        }
    }
}.toMutableMap()

fun DataSlice?.isTranslatedObject(id: Value.GlobalId): Boolean = this
    ?.get(id.value)
    ?.get("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
    ?.contains(Value.GlobalId("https://ns.ontola.io/libro/TranslatedObject"))
    ?: false
