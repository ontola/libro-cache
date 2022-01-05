package io.ontola.libro.metadata

import io.ontola.rdf.hextuples.Hextuple

fun findValue(data: List<Hextuple>, predicates: Array<String>, preferredLang: String): String? = data
    .filter { predicates.contains(it.predicate) && it.value.isNotBlank() }
    .minByOrNull { if (preferredLang == it.language) 0 else 1 }
    ?.value
