package io.ontola.cache.bulk

import io.ktor.http.HttpStatusCode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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

@OptIn(ExperimentalContracts::class)
internal fun CacheEntry?.isEmptyOrNotPublic(): Boolean {
    contract { returns(false) implies (this@isEmptyOrNotPublic != null) }

    return this == null || isNotPublic() || contents.isNullOrEmpty()
}

internal fun CacheEntry.isNotPublic(): Boolean {
    return cacheControl != CacheControl.Public
}
