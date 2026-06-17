package com.example.cafeypan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_pending")
data class SyncPendingEntity(
    @PrimaryKey(autoGenerate = true)
    val syncId: Int = 0,
    val taskId: Int, // ID local temporal o ID real de la tarea
    val operacion: String, // "INSERT", "COMPLETE", "UPDATE", "DELETE"
    val descripcion: String? = null,
    val fecha: String? = null,
    val rol: String? = null,
    val fechaCompletado: String? = null,
    val asignadoAId: Int? = null,
    val asignadoANombre: String? = null,
    val completadoPorNombre: String? = null,
    val fechaLimite: String? = null,
    val durationMinutes: Int? = null,
    val notas: String? = null
)
