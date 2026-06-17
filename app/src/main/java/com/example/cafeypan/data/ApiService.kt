package com.example.cafeypan.data

import com.example.cafeypan.model.RespuestaBasica
import com.example.cafeypan.model.RespuestaLogin
import com.example.cafeypan.model.RespuestaTareas
import com.example.cafeypan.model.RespuestaMermaSemanal
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @FormUrlEncoded
    @POST("login.php")
    suspend fun verificarPin(@Field("pin") pin: String): Response<RespuestaLogin>

    @FormUrlEncoded
    @POST("guardar_tarea.php")
    suspend fun guardarTarea(
        @Field("descripcion") descripcion: String,
        @Field("fecha") fecha: String,
        @Field("rol") rol: String,
        @Field("asignadoAId") asignadoAId: Int?,
        @Field("asignadoANombre") asignadoANombre: String?,
        @Field("fechaLimite") fechaLimite: String?,
        @Field("duracionMinutos") duracionMinutos: Int?,
        @Field("notas") notas: String?
    ): Response<RespuestaBasica>

    @GET("obtener_tareas.php")
    suspend fun obtenerListaTareas(@retrofit2.http.Query("fecha") fecha: String): Response<RespuestaTareas>

    @FormUrlEncoded
    @POST("completar_tarea.php")
    suspend fun completarTarea(
        @Field("id") id: Int,
        @Field("completadoPorNombre") completadoPorNombre: String?
    ): Response<RespuestaBasica>

    @FormUrlEncoded
    @POST("eliminar_tarea.php")
    suspend fun eliminarTarea(@Field("id") id: Int): Response<RespuestaBasica>

    @FormUrlEncoded
    @POST("editar_tarea.php")
    suspend fun editarTarea(
        @Field("id") id: Int,
        @Field("descripcion") descripcion: String,
        @Field("rol") rol: String?,
        @Field("asignadoAId") asignadoAId: Int?,
        @Field("asignadoANombre") asignadoANombre: String?,
        @Field("fecha") fecha: String?,
        @Field("fechaLimite") fechaLimite: String?,
        @Field("duracionMinutos") duracionMinutos: Int?,
        @Field("notas") notas: String?
    ): Response<RespuestaBasica>

    @GET("obtener_historial.php")
    suspend fun obtenerHistorialTareas(): Response<RespuestaTareas>

    @FormUrlEncoded
    @POST("guardar_trabajador.php")
    suspend fun guardarTrabajador(
        @Field("nombre") nombre: String,
        @Field("pin") pin: String,
        @Field("rol") rol: String,
        @Field("apellido") apellido: String?,
        @Field("telefono") telefono: String?,
        @Field("contactoEmergenciaNombre") contactoEmergenciaNombre: String?,
        @Field("contactoEmergenciaTelefono") contactoEmergenciaTelefono: String?,
        @Field("notas") notas: String?
    ): Response<RespuestaLogin>

    @GET("obtener_trabajadores.php")
    suspend fun obtenerTrabajadores(): Response<List<com.example.cafeypan.data.local.entity.UserEntity>>

    @FormUrlEncoded
    @POST("toggle_trabajador.php")
    suspend fun toggleTrabajador(
        @Field("id") id: Int,
        @Field("activo") activo: Boolean
    ): Response<RespuestaBasica>

    @FormUrlEncoded
    @POST("editar_trabajador.php")
    suspend fun editarTrabajador(
        @Field("id") id: Int,
        @Field("nombre") nombre: String,
        @Field("pin") pin: String,
        @Field("rol") rol: String,
        @Field("apellido") apellido: String?,
        @Field("telefono") telefono: String?,
        @Field("contactoEmergenciaNombre") contactoEmergenciaNombre: String?,
        @Field("contactoEmergenciaTelefono") contactoEmergenciaTelefono: String?,
        @Field("notas") notas: String?
    ): Response<RespuestaLogin>

    @FormUrlEncoded
    @POST("guardar_merma.php")
    suspend fun guardarMerma(
        @Field("product") product: String,
        @Field("quantity") quantity: Double,
        @Field("reason") reason: String,
        @Field("cost") cost: Double,
        @Field("date") date: String
    ): Response<RespuestaBasica>

    @GET("obtener_merma_semanal.php")
    suspend fun obtenerMermaSemanal(): Response<RespuestaMermaSemanal>
}