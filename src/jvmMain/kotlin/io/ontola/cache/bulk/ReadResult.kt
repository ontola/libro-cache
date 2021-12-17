package io.ontola.cache.bulk

internal class ReadResult {
    private val _notCached = mutableListOf<CacheRequest>()
    var notCached: List<CacheRequest> = _notCached
        get() = field.toList()

    private val _cachedNotPublic = mutableListOf<CacheEntry>()
    var cachedNotPublic: List<CacheEntry> = _cachedNotPublic
        get() = field.toList()

    private val _cachedPublic = mutableListOf<CacheEntry>()
    var cachedPublic: List<CacheEntry> = _cachedPublic
        get() = field.toList()

    val entirelyPublic: Boolean
        get() = _notCached.isEmpty() && _cachedNotPublic.isEmpty()

    val stats: Stats
        get() = Stats(
            cached = cachedNotPublic.size,
            authorized = notCached.size,
            public = cachedPublic.size,
        )

    internal fun add(entry: CacheEntry) {
        if (entry.isNotPublic()) {
            _cachedNotPublic.add(entry)
        } else {
            _cachedPublic.add(entry)
        }
    }

    internal fun add(entry: CacheRequest) {
        _notCached.add(entry)
    }
}
