package io.ontola.cache.bulk

import io.ktor.application.ApplicationCall
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import java.io.Writer

suspend fun ApplicationCall.socketHandler(
    requested: List<CacheRequest>,
    outgoing: SendChannel<Frame>,
) {
    val result = readAndPartition(requested)

    val writer = object : Writer() {
        override fun close() {
            /* no-op */
        }

        override fun flush() {
            /* no-op */
        }

        override fun write(cbuf: CharArray, off: Int, len: Int) {
            runBlocking {
                outgoing.send(Frame.Text(String(cbuf)))
            }
        }
    }

    readOrFetch(result, writer)
}
