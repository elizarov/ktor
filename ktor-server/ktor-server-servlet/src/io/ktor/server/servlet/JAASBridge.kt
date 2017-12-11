package io.ktor.server.servlet

import io.ktor.request.ApplicationRequest
import java.security.Principal

val ApplicationRequest.javaSecurityPrincipal: Principal?
    get() = when (this) {
        is ServletApplicationRequest -> servletRequest.userPrincipal
        else -> null
    }
