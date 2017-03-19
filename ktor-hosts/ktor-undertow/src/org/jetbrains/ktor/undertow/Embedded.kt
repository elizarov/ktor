package org.jetbrains.ktor.undertow

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.host.*

fun embeddedUndertowServer(port: Int = 80, host: String = "0.0.0.0", configure: Application.() -> Unit): UndertowApplicationHost {
    val hostConfig = applicationHostConfig {
        connector {
            this.port = port
            this.host = host
        }
    }

    val applicationConfig = applicationEnvironment {}
    return embeddedUndertowServer(hostConfig, applicationConfig, configure)
}

fun embeddedUndertowServer(port: Int = 80, host: String = "0.0.0.0", application: Application): UndertowApplicationHost {
    val hostConfig = applicationHostConfig {
        connector {
            this.port = port
            this.host = host
        }
    }

    val applicationConfig = applicationEnvironment {}
    return embeddedUndertowServer(hostConfig, applicationConfig, application)
}

fun embeddedUndertowServer(hostConfig: ApplicationHostConfig, environment: ApplicationEnvironment, application: Application): UndertowApplicationHost {
    return UndertowApplicationHost(hostConfig, environment, object : ApplicationLifecycle {
        override val application: Application = application
        override fun onBeforeInitializeApplication(initializer: Application.() -> Unit) {
            application.initializer()
        }

        override fun dispose() = application.dispose()
    })
}

fun embeddedUndertowServer(hostConfig: ApplicationHostConfig, environment: ApplicationEnvironment, configure: Application.() -> Unit): UndertowApplicationHost {
    return embeddedUndertowServer(hostConfig, environment, Application(environment, Unit).apply(configure))
}
