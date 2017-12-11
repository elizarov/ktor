package io.ktor.server.servlet

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.cio.CIOWriteChannelAdapter
import io.ktor.cio.ChannelWriteException
import io.ktor.cio.ReadChannel
import io.ktor.cio.WriteChannel
import io.ktor.content.IncomingContent
import io.ktor.content.OutgoingContent
import io.ktor.request.ApplicationRequest
import io.ktor.request.MultiPartData
import io.ktor.response.ApplicationResponse
import io.ktor.response.ApplicationSendPipeline
import io.ktor.response.ResponsePushBuilder
import io.ktor.server.engine.BaseApplicationCall
import kotlinx.coroutines.experimental.runBlocking
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Method
import java.nio.ByteBuffer
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.coroutines.experimental.CoroutineContext

open class AsyncServletApplicationCall(
    application: Application,
    servletRequest: HttpServletRequest,
    servletResponse: HttpServletResponse,
    engineContext: CoroutineContext,
    userContext: CoroutineContext,
    upgrade: ServletUpgrade
) : BaseApplicationCall(application) {
    override val request: ApplicationRequest = AsyncServletApplicationRequest(this, servletRequest)
    override val response: ApplicationResponse = AsyncServletApplicationResponse(this,
        servletRequest, servletResponse, engineContext, userContext, upgrade)
}

private class AsyncServletApplicationRequest(
    call: ApplicationCall,
    servletRequest: HttpServletRequest
) : ServletApplicationRequest(call, servletRequest) {
    override fun receiveContent() = AsyncServletIncomingContent(this)
}

private class AsyncServletIncomingContent(
    override val request: AsyncServletApplicationRequest
) : IncomingContent {
    private val copyJob by lazy { servletReader(request.servletRequest.inputStream) }

    override fun readChannel(): ReadChannel = object: ReadChannel {
        suspend override fun read(dst: ByteBuffer): Int {
            return copyJob.channel.readAvailable(dst)
        }

        override fun close() {
            runBlocking {
                copyJob.cancel()
                copyJob.join()
            }
        }
    }

    override fun multiPartData(): MultiPartData = ServletMultiPartData(request)
    override fun inputStream(): InputStream = request.servletRequest.inputStream
}

open class AsyncServletApplicationResponse(
    call: AsyncServletApplicationCall,
    private val servletRequest: HttpServletRequest,
    servletResponse: HttpServletResponse,
    protected val engineContext: CoroutineContext,
    protected val userContext: CoroutineContext,
    private val servletUpgradeImpl: ServletUpgrade
) : ServletApplicationResponse(call, servletResponse) {
    private val responseByteChannel = lazy {
        servletWriter(servletResponse.outputStream)
    }

    private val responseChannel = lazy {
        CIOWriteChannelAdapter(responseByteChannel.value.channel)
    }

    override suspend fun responseChannel(): WriteChannel = responseChannel.value

    @Volatile
    private var completed: Boolean = false

    suspend final override fun respondUpgrade(upgrade: OutgoingContent.ProtocolUpgrade) {
        try {
            servletResponse.flushBuffer()
        } catch (e: IOException) {
            throw ChannelWriteException("Cannot write HTTP upgrade response", e)
        }

        completed = true

        servletUpgradeImpl.performUpgrade(upgrade, servletRequest, servletResponse, engineContext, userContext)
    }

    init {
        pipeline.intercept(ApplicationSendPipeline.Engine) {
            if (!completed) {
                completed = true
                if (responseByteChannel.isInitialized()) {
                    responseByteChannel.value.apply {
                        channel.close()
                        join()
                    }
                } else {
                    servletResponse.flushBuffer()
                }
            }
        }
    }

    override fun push(builder: ResponsePushBuilder) {
        if (!tryPush(servletRequest, builder)) {
            super.push(builder)
        }
    }

    private fun tryPush(request: HttpServletRequest, builder: ResponsePushBuilder): Boolean {
        return foundPushImpls.any { function ->
            tryInvoke(function, request, builder)
        }
    }

    companion object {
        private val foundPushImpls by lazy {
            listOf("io.ktor.servlet.v4.PushKt.doPush").mapNotNull { tryFind(it) }
        }

        private fun tryFind(spec: String): Method? = try {
            require("." in spec)
            val methodName = spec.substringAfterLast(".")

            Class.forName(spec.substringBeforeLast(".")).methods.singleOrNull { it.name == methodName }
        } catch (ignore: ReflectiveOperationException) {
            null
        } catch (ignore: LinkageError) {
            null
        }

        private fun tryInvoke(function: Method, request: HttpServletRequest, builder: ResponsePushBuilder) = try {
            function.invoke(null, request, builder) as Boolean
        } catch (ignore: ReflectiveOperationException) {
            false
        } catch (ignore: LinkageError) {
            false
        }
    }
}
