package io.ktor.server.jetty

import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.execute
import io.ktor.response.respond
import io.ktor.server.engine.ApplicationEngineEnvironment
import io.ktor.server.engine.EnginePipeline
import io.ktor.util.DispatcherWithShutdown
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import java.util.concurrent.ScheduledThreadPoolExecutor
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal class JettyKtorHandler(val environment: ApplicationEngineEnvironment, private val pipeline: () -> EnginePipeline, private val engineDispatcher: CoroutineDispatcher) : AbstractHandler() {
    private val executor = ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 8)
    private val dispatcher = DispatcherWithShutdown(executor.asCoroutineDispatcher())
    private val MULTI_PART_CONFIG = MultipartConfigElement(System.getProperty("java.io.tmpdir"))

    override fun destroy() {
        dispatcher.prepareShutdown()
        try {
            super.destroy()
            executor.shutdownNow()
        } finally {
            dispatcher.completeShutdown()
        }
    }

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        val call = JettyApplicationCall(environment.application, baseRequest, request, response,
            engineContext = engineDispatcher, userContext = dispatcher)

        try {
            val contentType = request.contentType
            if (contentType != null && contentType.startsWith("multipart/")) {
                baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG)
                // TODO someone reported auto-cleanup issues so we have to check it
            }

            request.startAsync()?.apply {
                timeout = 0 // Overwrite any default non-null timeout to prevent multiple dispatches
            }
            baseRequest.isHandled = true

            launch(dispatcher) {
                try {
                    pipeline().execute(call)
                } finally {
                    request.asyncContext?.complete()
                }
            }
        } catch(ex: Throwable) {
            environment.log.error("Application ${environment.application::class.java} cannot fulfill the request", ex)

            launch(dispatcher) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}