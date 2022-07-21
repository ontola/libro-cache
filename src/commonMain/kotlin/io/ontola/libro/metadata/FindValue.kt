package io.ontola.libro.metadata

import io.ktor.http.Url
import io.ontola.empathy.web.Record
import io.ontola.empathy.web.Value
import io.ontola.empathy.web.shortenedGlobalIdString

internal fun findValue(data: Record?, predicates: Array<String>, preferredLang: String, websiteIRI: Url?): String? {
    if (data == null) return null

    return predicates
        .flatMap { data[shortenedGlobalIdString(it, websiteIRI)] ?: emptyList() }
        .filter { it.value.isNotBlank() }
        .minByOrNull { if (it is Value.LangString && preferredLang == it.lang) 0 else 1 }
        ?.value
}
