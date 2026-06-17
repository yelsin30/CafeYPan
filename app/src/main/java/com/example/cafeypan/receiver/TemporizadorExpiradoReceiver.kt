package com.example.cafeypan.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cafeypan.R

class TemporizadorExpiradoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("task_id", 0)
        val taskDesc = intent.getStringExtra("task_desc") ?: "Temporizador terminado"

        val notification = NotificationCompat.Builder(context, "canal_cafeteria")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¡Temporizador Terminado! ⏱️")
            .setContentText("Tu temporizador para '$taskDesc' ha finalizado.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(taskId + 10000, notification) // ID offset to prevent collisions
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
