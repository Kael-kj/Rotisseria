package com.kenji.rotisseria00.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kenji.rotisseria00.MainActivity
import com.kenji.rotisseria00.network.RotisseriaApi
import com.kenji.rotisseria00.utils.SoundPlayer
import kotlinx.coroutines.*

class PedidoNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null
    
    // Conjunto para evitar duplicar notificações do mesmo item pronto
    private val itensNotificados = mutableSetOf<String>()

    companion object {
        const val CHANNEL_ID = "RotisseriaPedidosChannel"
        const val NOTIFICATION_ID = 1001
        
        fun start(context: Context) {
            val intent = Intent(context, PedidoNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PedidoNotificationService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createServiceNotification("Monitorando pedidos prontos...")
        startForeground(NOTIFICATION_ID, notification)
        
        iniciarMonitoramento()
        
        return START_STICKY
    }

    private fun iniciarMonitoramento() {
        job?.cancel()
        job = serviceScope.launch {
            while (isActive) {
                try {
                    val comandas = RotisseriaApi.buscarContasAbertas()
                    
                    comandas.forEach { comanda ->
                        comanda.itens.forEach { item ->
                            val chaveItem = "${comanda.mesa}-${item.nome}"
                            
                            if ((item.statusCozinha == "PRONTO") && (chaveItem !in itensNotificados)) {
                                mostrarNotificacaoPedidoPronto(comanda.mesa, item.nome)
                                itensNotificados.add(chaveItem)
                            } else if (item.statusCozinha != "PRONTO") {
                                // Se o item sumiu ou mudou de status, removemos do conjunto para permitir nova notificação no futuro
                                itensNotificados.remove(chaveItem)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(10000) // Verifica a cada 10 segundos
            }
        }
    }

    private fun mostrarNotificacaoPedidoPronto(mesa: String, itemNome: String) {
        SoundPlayer.playNotificationSound(this)
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pedido PRONTO - Mesa $mesa")
            .setContentText("O item '$itemNome' está pronto para ser servido!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createServiceNotification(texto: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rotisseria - Garçom")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val name = "Avisos de Pedidos"
        val descriptionText = "Notificações de pedidos prontos na cozinha"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
