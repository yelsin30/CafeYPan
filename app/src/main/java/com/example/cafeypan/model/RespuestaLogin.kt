package com.example.cafeypan.model

data class RespuestaLogin(
    val exito: Boolean,
    val mensaje: String,
    val datos: Trabajador?
)

data class Trabajador(
    val id: Int,
    val nombre: String,
    val rol: String,
    val pin: String,
    val apellido: String? = null,
    val telefono: String? = null,
    val contactoEmergenciaNombre: String? = null,
    val contactoEmergenciaTelefono: String? = null,
    val notas: String? = null
)