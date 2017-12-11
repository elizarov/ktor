package io.ktor.server.jetty

import io.ktor.application.Application
import io.ktor.server.jetty.internal.JettyUpgradeImpl
import io.ktor.server.servlet.AsyncServletApplicationCall
import org.eclipse.jetty.server.Request
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.coroutines.experimental.CoroutineContext

class JettyApplicationCall(
    application: Application,
    request: Request,
    servletRequest: HttpServletRequest,
    servletResponse: HttpServletResponse,
    engineContext: CoroutineContext,
    userContext: CoroutineContext
) : AsyncServletApplicationCall(application, servletRequest, servletResponse, engineContext, userContext, JettyUpgradeImpl) {
    override val response: JettyApplicationResponse =
            JettyApplicationResponse(this,
                    servletRequest,
                    servletResponse,
                    engineContext,
                    userContext,
                    request)
}