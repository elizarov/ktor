package org.jetbrains.ktor.undertow.tests

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.testing.*
import org.jetbrains.ktor.undertow.*

class UndertowHostTest : HostTestSuite<UndertowApplicationHost>() {
    override fun createServer(envInit: ApplicationEnvironmentBuilder.() -> Unit, routing: Routing.() -> Unit): UndertowApplicationHost {
        val config = hostConfig(port, sslPort)
        val env = applicationEnvironment(envInit)

        return embeddedUndertowServer(config, env) {
            install(Routing.Feature, routing)
        }
    }
}