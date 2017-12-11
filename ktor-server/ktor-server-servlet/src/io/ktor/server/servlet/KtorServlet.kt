package io.ktor.server.servlet

import io.ktor.application.Application
import io.ktor.application.log
import io.ktor.pipeline.execute
import io.ktor.server.engine.EnginePipeline
import io.ktor.util.DispatcherWithShutdown
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

abstract class KtorServlet : HttpServlet() {
    private val asyncDispatchers = lazy { AsyncDispatchers() }

    abstract val application: Application
    abstract val enginePipeline: EnginePipeline

    abstract val upgrade: ServletUpgrade

    override fun destroy() {
        // Note: container will not call service again, so asyncDispatcher cannot get initialized if it was not yet
        if (asyncDispatchers.isInitialized()) asyncDispatchers.value.destroy()
    }

    override fun service(request: HttpServletRequest, response: HttpServletResponse) {
        if (response.isCommitted) return
        response.characterEncoding = "UTF-8"
        request.characterEncoding = "UTF-8"
        try {
            if (request.isAsyncSupported) {
                asyncService(request, response)
            } else {
                blockingService(request, response)
            }
        } catch (ex: Throwable) {
            application.log.error("ServletApplicationEngine cannot service the request", ex)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.message)
        }
    }

    private fun asyncService(request: HttpServletRequest, response: HttpServletResponse) {
        val asyncContext = request.startAsync()!!.apply {
            timeout = 0L
        }
        val ad = asyncDispatchers.value
        val call = AsyncServletApplicationCall(application, request, response,
            engineContext = ad.engineDispatcher, userContext = ad.dispatcher, upgrade = upgrade)
        launch(ad.dispatcher) {
            try {
                enginePipeline.execute(call)
            } finally {
                asyncContext.complete()
            }
        }
    }

    private fun blockingService(request: HttpServletRequest, response: HttpServletResponse) {
        runBlocking {
            val call = BlockingServletApplicationCall(application, request, response)
            enginePipeline.execute(call)
        }
    }
}

private class AsyncDispatchers {
    val engineExecutor = ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors())
    val engineDispatcher = DispatcherWithShutdown(engineExecutor.asCoroutineDispatcher())

    val executor = ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 8)
    val dispatcher = DispatcherWithShutdown(executor.asCoroutineDispatcher())

    fun destroy() {
        engineDispatcher.prepareShutdown()
        dispatcher.prepareShutdown()
        try {
            executor.shutdownNow()
            engineExecutor.shutdown()
            executor.awaitTermination(1L, TimeUnit.SECONDS)
            engineExecutor.awaitTermination(1L, TimeUnit.SECONDS)
        } finally {
            engineDispatcher.completeShutdown()
            dispatcher.completeShutdown()
        }
    }
}