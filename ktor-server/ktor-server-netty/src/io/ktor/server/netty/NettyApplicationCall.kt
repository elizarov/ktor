package io.ktor.server.netty

import io.ktor.application.Application
import io.ktor.server.engine.BaseApplicationCall
import io.netty.channel.ChannelHandlerContext
import io.netty.util.ReferenceCountUtil
import kotlinx.coroutines.experimental.Job

internal abstract class NettyApplicationCall(
    application: Application,
    val context: ChannelHandlerContext,
    private val requestMessage: Any
) : BaseApplicationCall(application) {
    override abstract val request: NettyApplicationRequest
    override abstract val response: NettyApplicationResponse

    internal val responseWriteJob = Job()

    internal suspend fun finish() {
        try {
            response.ensureResponseSent()
            responseWriteJob.join()
        } finally {
            request.close()
            ReferenceCountUtil.release(requestMessage)
        }
    }

    internal fun dispose() {
        response.close()
        request.close()
        ReferenceCountUtil.release(requestMessage)
    }
}