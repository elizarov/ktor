package org.jetbrains.ktor.jetty

import org.eclipse.jetty.io.*
import org.eclipse.jetty.util.*
import org.jetbrains.ktor.cio.*
import java.nio.*
import kotlin.coroutines.experimental.*

internal class EndPointWriteChannel(val endPoint: EndPoint) : WriteChannel {
    @Volatile
    private var handler: Continuation<Unit>? = null

    private val callback = object : Callback {
        override fun succeeded() {
            val h = handler

            handler = null

            h?.resume(Unit)
        }

        override fun failed(x: Throwable?) {
            handler?.resumeWithException(x ?: Exception())
        }
    }

    suspend override fun write(src: ByteBuffer) {
        if (!src.hasRemaining()) return

        return suspendCoroutine { continuation ->
            this.handler = continuation
            endPoint.write(callback, src)
        }
    }

    suspend override fun flush() {
    }

    override fun close() {
        endPoint.close()
    }
}