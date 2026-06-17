package com.example.cafeypan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val rol: String, // "Dueño", "Barista", "Panadero", "Cajero"
    val pin: String,
    val activo: Boolean = true,
    val apellido: String? = null,
    val telefono: String? = null,
    val contactoEmergenciaNombre: String? = null,
    val contactoEmergenciaTelefono: String? = null,
    val notas: String? = null
)
