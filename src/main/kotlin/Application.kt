package com

import io.ktor.server.application.Application
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import java.net.InetAddress

fun Application.rootModule() {
    configureResources()
    configureSerialization()
    configureRouting()
}

fun iniciarAutoDiscovery() {
    val jmdns = JmDNS.create(InetAddress.getLocalHost())
    val serviceInfo = ServiceInfo.create("_http._tcp.local.", "rotisseria-server", 8080, "Servidor da Rotisseria")
    jmdns.registerService(serviceInfo)
}