package io.ktor.server.servlet

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.cio.ReadChannel
import io.ktor.cio.ReadChannelFromInputStream
import io.ktor.cio.WriteChannel
import io.ktor.content.IncomingContent
import io.ktor.content.OutgoingContent
import io.ktor.request.ApplicationRequest
import io.ktor.request.MultiPartData
import io.ktor.response.ApplicationResponse
import io.ktor.server.engine.BaseApplicationCall
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class BlockingServletApplicationCall(
    application: Application,
    servletRequest: HttpServletRequest,
    servletResponse: HttpServletResponse
) : BaseApplicationCall(application) {
    override val request: ApplicationRequest = BlockingServletApplicationRequest(this, servletRequest)
    override val response: ApplicationResponse = BlockingServletApplicationResponse(this, servletResponse)
}

private class BlockingServletApplicationRequest(
    call: ApplicationCall,
    servletRequest: HttpServletRequest
) : ServletApplicationRequest(call, servletRequest) {
    override fun receiveContent(): IncomingContent = BlockingServletIncomingContent(this)
}

private class BlockingServletIncomingContent(
    override val request: BlockingServletApplicationRequest
) : IncomingContent {
    override fun inputStream(): InputStream = request.servletRequest.inputStream!!
    override fun readChannel(): ReadChannel = ReadChannelFromInputStream(inputStream())
    override fun multiPartData(): MultiPartData = ServletMultiPartData(request)
}

private class BlockingServletApplicationResponse(
    call: ApplicationCall,
    servletResponse: HttpServletResponse
) : ServletApplicationResponse(call, servletResponse) {
    override suspend fun responseChannel(): WriteChannel =
        BlockingServletWriteChannel(servletResponse.outputStream)

    suspend override fun respondUpgrade(upgrade: OutgoingContent.ProtocolUpgrade) {
        servletResponse.sendError(501, "Upgrade is not supported in synchronous servlets")
    }
}

private class BlockingServletWriteChannel(private val outputStream: OutputStream) : WriteChannel {
    suspend override fun write(src: ByteBuffer) {
        // todo: what if bytebuffer is not backed by array?
        outputStream.write(src.array(), src.arrayOffset() + src.position(), src.remaining())
        src.position(src.limit())
    }

    suspend override fun flush() {
        outputStream.flush()
    }

    override fun close() {
        outputStream.close()
    }
}

