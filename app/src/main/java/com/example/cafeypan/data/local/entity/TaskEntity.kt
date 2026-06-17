package com.example.cafeypan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tareas")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val descripcion: String,
    val estado: String = "Pendiente", // "Pendiente", "Completada"
    val fecha: String,
    val fechaCompletado: String? = null,
    val rol: String,
    val asignadoAId: Int? = null,
    val asignadoANombre: String? = null,
    val completadoPorNombre: String? = null,
    val fechaLimite: String? = null,
    val durationMinutes: Int? = null,
    val notas: String? = null
)
