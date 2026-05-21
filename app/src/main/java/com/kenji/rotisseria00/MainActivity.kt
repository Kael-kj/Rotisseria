package com.kenji.rotisseria00

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.kenji.rotisseria00.network.RotisseriaApi
import com.kenji.rotisseria00.network.ServerDiscovery
import com.kenji.rotisseria00.services.PedidoNotificationService
import com.kenji.rotisseria00.ui.AppNavigation // Importe o arquivo que criamos
import com.kenji.rotisseria00.ui.theme.Rotisseria00Theme

class MainActivity : ComponentActivity() {

    private var serverDiscovery: ServerDiscovery? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permissão de notificação tratada
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        checkNotificationPermission()
        setupServerDiscovery()

        setContent {
            Rotisseria00Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        onStartService = { PedidoNotificationService.start(this) },
                        onStopService = { PedidoNotificationService.stop(this) }
                    )
                }
            }
        }
    }

    private fun setupServerDiscovery() {
        val wifi = getSystemService(WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("RotisseriaMulticastLock").apply {
            setReferenceCounted(true)
            acquire()
        }

        serverDiscovery = ServerDiscovery(this) { baseUrl ->
            Log.d("Rotisseria", "Servidor encontrado via NSD: $baseUrl")
            RotisseriaApi.updateBaseUrl(baseUrl)
        }
        serverDiscovery?.startDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        serverDiscovery?.stopDiscovery()
        multicastLock?.let {
            if (it.isHeld) it.release()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}