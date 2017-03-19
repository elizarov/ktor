@file:JvmName("DevelopmentHost")

package org.jetbrains.ktor.undertow

import org.jetbrains.ktor.host.*

fun main(args: Array<String>) {
    val (applicationHostConfig, applicationEnvironment) = commandLineConfig(args)
    UndertowApplicationHost(applicationHostConfig, applicationEnvironment).start()
}
