package com.kenji.rotisseria00.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class ServerDiscovery(context: Context, private val onServerFound: (String) -> Unit) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d("NSD", "Discovery started: $regType")
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.d("NSD", "Service found: ${serviceInfo.serviceName}")
            // Quando encontrar o servidor, resolvemos o IP
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val ip = serviceInfo.host.hostAddress
                    val port = serviceInfo.port
                    Log.d("NSD", "Service resolved: http://$ip:$port")
                    onServerFound("http://$ip:$port")
                }

                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                    Log.e("NSD", "Resolve failed: $errorCode")
                }
            })
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.d("NSD", "Service lost: ${serviceInfo.serviceName}")
        }

        override fun onDiscoveryStopped(regType: String) {
            Log.d("NSD", "Discovery stopped: $regType")
        }

        override fun onStartDiscoveryFailed(regType: String, errorCode: Int) {
            Log.e("NSD", "Start discovery failed: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(regType: String, errorCode: Int) {
            Log.e("NSD", "Stop discovery failed: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    fun startDiscovery() {
        Log.d("NSD", "Starting discovery for _http._tcp.")
        nsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
