package com.example.cafeypan.model

data class RespuestaTareas(
    val exito: Boolean,
    val datos: List<TareaItem>?
)

data class TareaItem(
    val id: Int,
    val descripcion: String,
    val estado: String,
    val fecha: String,
    val fecha_completado: String? = null,
    val rol: String? = null,
    val asignado_a_id: Int? = null,
    val asignado_a_nombre: String? = null,
    val completado_por_nombre: String? = null,
    val fecha_limite: String? = null,
    val duracion_minutos: Int? = null,
    val notas: String? = null
)