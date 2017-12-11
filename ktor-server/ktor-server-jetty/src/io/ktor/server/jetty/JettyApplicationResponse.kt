package io.ktor.server.jetty

import io.ktor.response.ResponsePushBuilder
import io.ktor.server.jetty.internal.JettyUpgradeImpl
import io.ktor.server.servlet.AsyncServletApplicationCall
import io.ktor.server.servlet.AsyncServletApplicationResponse
import org.eclipse.jetty.server.Request
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.coroutines.experimental.CoroutineContext

class JettyApplicationResponse(
    call: AsyncServletApplicationCall,
    servletRequest: HttpServletRequest,
    servletResponse: HttpServletResponse,
    engineContext: CoroutineContext,
    userContext: CoroutineContext,
    private val baseRequest: Request
) : AsyncServletApplicationResponse(call, servletRequest, servletResponse, engineContext, userContext, JettyUpgradeImpl) {
    override fun push(builder: ResponsePushBuilder) {
        if (baseRequest.isPushSupported) {
            baseRequest.pushBuilder.apply {
                this.method(builder.method.value)
                this.path(builder.url.encodedPath)
                val query = builder.url.build().substringAfter('?', "").takeIf { it.isNotEmpty() }
                if (query != null) {
                    queryString(query)
                }

                push()
            }
        } else {
            super.push(builder)
        }
    }
}