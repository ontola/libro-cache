package tools.empathy.libro.server.bulk

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
//    write("${listOf(statusCode(entry.iri, entry.status)).toSlice()}\n")
    entry.contents?.let { contents ->
        write(Json.encodeToString(contents))
        write("\n")
    }
}
