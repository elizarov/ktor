package org.jetbrains.ktor.undertow

import io.undertow.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.transform.*
import java.util.concurrent.*
import javax.net.ssl.*

class UndertowApplicationHost(override val hostConfig: ApplicationHostConfig,
                              val environment: ApplicationEnvironment,
                              val lifecycle: ApplicationLifecycle) : ApplicationHostStartable {

    val pipeline = defaultHostPipeline(environment)
    val application: Application get() = lifecycle.application

    constructor(hostConfig: ApplicationHostConfig, environment: ApplicationEnvironment)
            : this(hostConfig, environment, ApplicationLoader(environment, hostConfig.autoreload))

    init {
        lifecycle.onBeforeInitializeApplication {
            install(ApplicationTransform).registerDefaultHandlers()
        }
    }

    override fun start(wait: Boolean): ApplicationHostStartable {
        server.start()
        return this
    }

    override fun stop(gracePeriod: Long, timeout: Long, timeUnit: TimeUnit) {
        server.stop()
    }

    val server = Undertow.builder().apply {
        hostConfig.connectors.map { connector ->
            when (connector.type) {
                ConnectorType.HTTP -> addHttpListener(connector.port, connector.host)
                ConnectorType.HTTPS -> addHttpsListener(connector.port, connector.host, sslContext(connector as HostSSLConnectorConfig))
                else -> throw IllegalArgumentException("Connector type ${connector.type} is not supported by Undertow host implementation")
            }
        }

        setHandler(UndertowHttpHandler(this@UndertowApplicationHost))
    }.build()

    internal val callWorker = ForkJoinPool() // processes calls

    private fun sslContext(connector: HostSSLConnectorConfig): SSLContext {
        val keyManagers: Array<KeyManager>
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(connector.keyStore, connector.keyStorePassword())
        keyManagers = keyManagerFactory.keyManagers

        val trustManagers: Array<TrustManager>
        val trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(connector.keyStore)
        trustManagers = trustManagerFactory.trustManagers

        val sslContext: SSLContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagers, trustManagers, null)
        return sslContext
    }

    override fun toString(): String {
        return "Undertow($hostConfig)"
    }
}

