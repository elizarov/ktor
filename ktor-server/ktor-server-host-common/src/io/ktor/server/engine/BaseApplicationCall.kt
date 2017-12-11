package io.ktor.server.engine

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.util.Attributes
import io.ktor.util.ValuesMap

/**
 * Base class for implementing an [ApplicationCall]
 */
abstract class BaseApplicationCall(final override val application: Application) : ApplicationCall {
    final override val attributes = Attributes()
    override val parameters: ValuesMap get() = request.queryParameters
}