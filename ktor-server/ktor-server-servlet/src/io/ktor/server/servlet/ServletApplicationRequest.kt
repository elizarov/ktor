package io.ktor.server.servlet

import io.ktor.application.ApplicationCall
import io.ktor.http.RequestConnectionPoint
import io.ktor.http.parseQueryString
import io.ktor.request.RequestCookies
import io.ktor.server.engine.BaseApplicationRequest
import io.ktor.util.ValuesMap
import javax.servlet.http.HttpServletRequest

abstract class ServletApplicationRequest(
    call: ApplicationCall,
    val servletRequest: HttpServletRequest
) : BaseApplicationRequest(call) {
    override val local: RequestConnectionPoint = ServletConnectionPoint(servletRequest)

    override val queryParameters by lazy {
        servletRequest.queryString?.let { parseQueryString(it) } ?: ValuesMap.Empty
    }

    override val headers: ValuesMap = ServletApplicationRequestHeaders(servletRequest)
    override val cookies: RequestCookies = ServletApplicationRequestCookies(servletRequest, this)
}