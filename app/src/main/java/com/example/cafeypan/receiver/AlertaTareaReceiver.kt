package com.example.cafeypan.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cafeypan.R

class AlertaTareaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val nombreTarea = intent.getStringExtra("nombreTarea") ?: "Tarea"
        val idTarea = intent.getIntExtra("idTarea", 0)

        // --- AQUÍ PREPARAMOS EL BOTÓN POSPONER ---
        val intentoPosponer = Intent(context, PosponerReceiver::class.java).apply {
            putExtra("idTarea", idTarea)
            putExtra("nombreTarea", nombreTarea)
        }
        val pasePosponer = PendingIntent.getBroadcast(
            context,
            idTarea,
            intentoPosponer,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Diseñamos cómo se verá el aviso, AHORA con el botón incluido
        val diseñoAviso = NotificationCompat.Builder(context, "canal_cafeteria")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¡Tiempo Excedido!")
            .setContentText("La tarea '$nombreTarea' lleva más de 10 minutos pendiente.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            // Esta es la línea mágica que dibuja el botón:
            .addAction(R.drawable.ic_launcher_foreground, "Posponer 10 min", pasePosponer)

        val lanzador = NotificationManagerCompat.from(context)
        try {
            lanzador.notify(idTarea, diseñoAviso.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}