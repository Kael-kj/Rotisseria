package com.kenji.rotisseria00.utils

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

object SoundPlayer {
    fun playNotificationSound(context: Context) {
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(context, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
