package io.ontola.cache.bulk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import java.io.OutputStream
import java.io.Writer
import java.nio.charset.Charset

suspend fun entriesToOutputStream(
    resources: Flow<CacheEntry>,
    outStream: OutputStream,
) {
    outStream.writer(Charset.defaultCharset()).use { writer ->
        resources.collect {
            writer.write(it)
        }
    }
}

private fun Writer.write(entry: CacheEntry) {
    write("${statusCode(entry.iri, entry.status)}\n")
    entry.contents?.let { contents ->
        write("$contents\n")
    }
}
