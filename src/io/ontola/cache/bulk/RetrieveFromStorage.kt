package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.util.pipeline.PipelineContext
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.KeyValue
import io.ontola.cache.features.cacheConfig
import io.ontola.cache.features.session
import io.ontola.cache.util.KeyManager
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

@OptIn(ExperimentalLettuceCoroutinesApi::class)
internal suspend fun PipelineContext<Unit, ApplicationCall>.retrieveFromStorage(
    requested: List<CacheRequest>,
    keyManager: KeyManager,
): MutableMap<String, CacheEntry> {
    val lang = call.session.language()
    val config = call.application.cacheConfig

    return requested
        .map { e -> keyManager.toKey(e.iri, lang) }
        .asFlow()
        .map { key -> key to config.cacheRedisConn.hmget(key, "iri", "status", "cacheControl", "contents") }
        .map { (key, hash) ->
            val test = hash.fold<KeyValue<String, String>, MutableMap<String, String>>(mutableMapOf()) { e, h ->
                h.ifHasValue { e[h.key] = it }
                e
            }

            keyManager.fromKey(key).first to test
        }
        .toList()
        .filter { (_, hash) -> hash.containsKey("iri") }
        .map { (key, hash) ->
            key to CacheEntry(
                iri = hash["iri"]!!,
                status = HttpStatusCode.fromValue(hash["status"]!!.toInt()),
                cacheControl = CacheControl.valueOf(hash["cacheControl"]!!),
                contents = hash["contents"],
            )
        }
        .associateBy({ it.first }, { it.second })
        .toMutableMap()
}
