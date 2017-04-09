package org.jetbrains.ktor.samples.httpbin

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.http.*

data class HttpBinError(
        val request: ApplicationRequest,
        val message: String,
        val code: HttpStatusCode,
        val cause: Throwable? = null
)