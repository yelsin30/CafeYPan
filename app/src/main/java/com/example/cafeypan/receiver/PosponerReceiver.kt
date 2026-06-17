package com.example.cafeypan.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

class PosponerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Rescatamos qué tarea estamos posponiendo
        val idTarea = intent.getIntExtra("idTarea", 0)
        val nombreTarea = intent.getStringExtra("nombreTarea") ?: "Tarea"

        // 2. Ocultamos el aviso actual de la pantalla
        NotificationManagerCompat.from(context).cancel(idTarea)

        // 3. Volvemos a programar la alarma original
        val administradorDeAlarmas = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val nuevoMensaje = Intent(context, AlertaTareaReceiver::class.java).apply {
            putExtra("idTarea", idTarea)
            putExtra("nombreTarea", nombreTarea)
        }

        val pase = PendingIntent.getBroadcast(
            context,
            idTarea,
            nuevoMensaje,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculamos: Hora de este momento + 10 minutos extra
        val tiempoExtra = System.currentTimeMillis() + (10 * 60 * 1000)

        administradorDeAlarmas.set(AlarmManager.RTC_WAKEUP, tiempoExtra, pase)

        // Le damos un pequeño mensaje al trabajador para que sepa que funcionó
        Toast.makeText(context, "Se dieron 10 minutos extra a: $nombreTarea", Toast.LENGTH_SHORT).show()
    }
}