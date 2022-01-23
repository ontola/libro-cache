package io.ontola.libro.metadata

import io.ontola.empathy.web.Record
import io.ontola.empathy.web.Value

fun findValue(data: Record?, predicates: Array<String>, preferredLang: String): String? {
    if (data == null) return null

    return predicates
        .flatMap { data[it]?.asList() ?: emptyList() }
        .filter { it.value.isNotBlank() }
        .minByOrNull { if (it is Value.LangString && preferredLang == it.lang) 0 else 1 }
        ?.value
}
