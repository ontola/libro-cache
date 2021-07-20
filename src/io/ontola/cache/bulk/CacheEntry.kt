package io.ontola.cache.bulk

import io.ktor.http.HttpStatusCode

data class CacheEntry(
    override val iri: String,
    val status: HttpStatusCode = HttpStatusCode.NotFound,
    val cacheControl: CacheControl = CacheControl.NoCache,
    val contents: String? = null,
) : CacheRequest(iri) {
    companion object {
        val fields = arrayOf("iri", "status", "cacheControl", "contents")
    }
}
