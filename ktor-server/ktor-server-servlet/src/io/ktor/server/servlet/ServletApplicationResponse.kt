package io.ktor.server.servlet

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.ResponseHeaders
import io.ktor.server.engine.BaseApplicationResponse
import javax.servlet.http.HttpServletResponse

abstract class ServletApplicationResponse(
    call: ApplicationCall,
    protected val servletResponse: HttpServletResponse
) : BaseApplicationResponse(call) {
    override fun setStatus(statusCode: HttpStatusCode) {
        servletResponse.status = statusCode.value
    }

    override val headers: ResponseHeaders = object : ResponseHeaders() {
        override fun engineAppendHeader(name: String, value: String) {
            servletResponse.addHeader(name, value)
        }

        override fun getEngineHeaderNames(): List<String> = servletResponse.headerNames.toList()
        override fun getEngineHeaderValues(name: String): List<String> = servletResponse.getHeaders(name).toList()
    }
}
