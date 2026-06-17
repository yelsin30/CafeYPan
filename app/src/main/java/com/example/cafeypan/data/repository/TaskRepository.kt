package com.example.cafeypan.data.repository

import com.example.cafeypan.data.ApiService
import com.example.cafeypan.data.local.dao.TaskDao
import com.example.cafeypan.data.local.dao.SyncPendingDao
import com.example.cafeypan.data.local.entity.TaskEntity
import com.example.cafeypan.data.local.entity.SyncPendingEntity
import com.example.cafeypan.model.RespuestaBasica
import com.example.cafeypan.model.RespuestaTareas
import com.example.cafeypan.model.TareaItem
import com.example.cafeypan.worker.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val apiService: ApiService,
    private val syncPendingDao: SyncPendingDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    fun getLocalTasksFlow(): Flow<List<TaskEntity>> = taskDao.getAllTasksFlow()
    fun getLocalTasksByDateFlow(date: String): Flow<List<TaskEntity>> = taskDao.getTasksByDateFlow(date)

    private suspend fun verificarTareasAtrasadasLocalmente() {
        val allTasks = taskDao.getAllTasks()
        val hoyStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        for (task in allTasks) {
            if (task.estado == "Pendiente" && task.fecha < hoyStr) {
                taskDao.insertTask(task.copy(estado = "Atrasada"))
            }
        }
    }

    suspend fun fetchAndSyncTasks(fecha: String): Result<List<TaskEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerListaTareas(fecha)
            if (response.isSuccessful && response.body() != null) {
                val remoteTasks = response.body()!!.datos ?: emptyList()
                
                // Limpiar base de datos local para la fecha específica excepto tareas pendientes de inserción
                val pendingOps = syncPendingDao.getAllPendingOperations()
                val pendingInsertTaskIds = pendingOps.filter { it.operacion == "INSERT" }.map { it.taskId }
                val keepIds = if (pendingInsertTaskIds.isEmpty()) listOf(-1) else pendingInsertTaskIds
                taskDao.clearTasksForDateExcept(fecha, keepIds)

                // Update local Room database
                for (task in remoteTasks) {
                    taskDao.insertTask(
                        TaskEntity(
                            id = task.id,
                            descripcion = task.descripcion,
                            estado = task.estado,
                            fecha = task.fecha,
                            fechaCompletado = task.fecha_completado,
                            rol = task.rol ?: "Empleado",
                            asignadoAId = task.asignado_a_id,
                            asignadoANombre = task.asignado_a_nombre,
                            completadoPorNombre = task.completado_por_nombre,
                            fechaLimite = task.fecha_limite,
                            durationMinutes = task.duracion_minutos,
                            notas = task.notas
                        )
                    )
                }
                verificarTareasAtrasadasLocalmente()
                val localTasks = taskDao.getAllTasks()
                return@withContext Result.success(localTasks)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        verificarTareasAtrasadasLocalmente()
        // Fallback to local Room database on network failure
        val localTasks = taskDao.getAllTasks()
        return@withContext Result.success(localTasks)
    }

    suspend fun agregarTarea(
        descripcion: String,
        fecha: String,
        rol: String,
        asignadoAId: Int? = null,
        asignadoANombre: String? = null,
        fechaLimite: String? = null,
        duracionMinutos: Int? = null,
        notas: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val fechas = if (fecha.contains(",")) {
            fecha.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            listOf(fecha.trim())
        }

        for (f in fechas) {
            // Save locally first
            val localTask = TaskEntity(
                descripcion = descripcion,
                fecha = f,
                rol = rol,
                estado = "Pendiente",
                asignadoAId = asignadoAId,
                asignadoANombre = asignadoANombre,
                fechaLimite = fechaLimite,
                durationMinutes = duracionMinutos,
                notas = notas
            )
            val insertedId = taskDao.insertTask(localTask).toInt()
            programarAlarmaTarea(insertedId, descripcion)

            // Encolar pendiente de sincronización
            syncPendingDao.insertPendingOperation(
                SyncPendingEntity(
                    taskId = insertedId,
                    operacion = "INSERT",
                    descripcion = descripcion,
                    fecha = f,
                    rol = rol,
                    asignadoAId = asignadoAId,
                    asignadoANombre = asignadoANombre,
                    fechaLimite = fechaLimite,
                    durationMinutes = duracionMinutos,
                    notas = notas
                )
            )
        }

        programarSincronizacion()
        return@withContext Result.success(true)
    }

    suspend fun completarTarea(id: Int, fechaCompletado: String, completadoPorNombre: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        // Fetch local task and update it
        val allTasks = taskDao.getAllTasks()
        val localTask = allTasks.find { it.id == id }
        if (localTask != null) {
            taskDao.insertTask(
                localTask.copy(
                    estado = "Completada",
                    fechaCompletado = fechaCompletado,
                    completadoPorNombre = completadoPorNombre
                )
            )
            cancelarAlarmaTarea(id)
        }

        // Encolar pendiente de sincronización
        syncPendingDao.insertPendingOperation(
            SyncPendingEntity(
                taskId = id,
                operacion = "COMPLETE",
                fechaCompletado = fechaCompletado,
                completadoPorNombre = completadoPorNombre
            )
        )

        programarSincronizacion()
        return@withContext Result.success(true)
    }

    suspend fun editarTarea(
        id: Int,
        descripcion: String,
        rol: String?,
        asignadoAId: Int?,
        asignadoANombre: String?,
        fecha: String?,
        fechaLimite: String?,
        notas: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val allTasks = taskDao.getAllTasks()
        val localTask = allTasks.find { it.id == id }
        if (localTask != null) {
            taskDao.insertTask(
                localTask.copy(
                    descripcion = descripcion,
                    rol = rol ?: localTask.rol,
                    asignadoAId = asignadoAId,
                    asignadoANombre = asignadoANombre,
                    fecha = fecha ?: localTask.fecha,
                    fechaLimite = fechaLimite,
                    notas = notas ?: localTask.notas
                )
            )
        }

        // Encolar pendiente de sincronización
        syncPendingDao.insertPendingOperation(
            SyncPendingEntity(
                taskId = id,
                operacion = "EDIT",
                descripcion = descripcion,
                rol = rol,
                asignadoAId = asignadoAId,
                asignadoANombre = asignadoANombre,
                fecha = fecha,
                fechaLimite = fechaLimite,
                notas = notas
            )
        )

        programarSincronizacion()
        return@withContext Result.success(true)
    }

    suspend fun eliminarTarea(id: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        taskDao.deleteTaskById(id)
        cancelarAlarmaTarea(id)

        // Encolar pendiente de sincronización
        syncPendingDao.insertPendingOperation(
            SyncPendingEntity(
                taskId = id,
                operacion = "DELETE"
            )
        )

        programarSincronizacion()
        return@withContext Result.success(true)
    }

    private fun programarSincronizacion() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sincronizacion_tareas",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    suspend fun fetchHistorial(): Result<List<TaskEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerHistorialTareas()
            if (response.isSuccessful && response.body() != null) {
                val remoteTasks = response.body()!!.datos ?: emptyList()

                // Limpiar base de datos local excepto tareas pendientes de inserción
                val pendingOps = syncPendingDao.getAllPendingOperations()
                val pendingInsertTaskIds = pendingOps.filter { it.operacion == "INSERT" }.map { it.taskId }
                val keepIds = if (pendingInsertTaskIds.isEmpty()) listOf(-1) else pendingInsertTaskIds
                taskDao.clearAllTasksExcept(keepIds)

                for (task in remoteTasks) {
                    taskDao.insertTask(
                        TaskEntity(
                            id = task.id,
                            descripcion = task.descripcion,
                            estado = task.estado,
                            fecha = task.fecha,
                            fechaCompletado = task.fecha_completado,
                            rol = task.rol ?: "Empleado",
                            asignadoAId = task.asignado_a_id,
                            asignadoANombre = task.asignado_a_nombre,
                            completadoPorNombre = task.completado_por_nombre,
                            fechaLimite = task.fecha_limite,
                            durationMinutes = task.duracion_minutos,
                            notas = task.notas
                        )
                    )
                }
                verificarTareasAtrasadasLocalmente()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        verificarTareasAtrasadasLocalmente()
        return@withContext Result.success(taskDao.getAllTasks())
    }

    suspend fun verificarYGenerarTareasRecurrentes(fechaHoy: String) = withContext(Dispatchers.IO) {
        val sharedPrefs = context.getSharedPreferences("cafeypan_prefs", android.content.Context.MODE_PRIVATE)
        val ultimaFechaGeneracion = sharedPrefs.getString("PREF_LAST_RECURRENT_GENERATION_DATE", "")
        
        if (ultimaFechaGeneracion == fechaHoy) {
            return@withContext
        }

        val tareasHoy = taskDao.getTasksByDate(fechaHoy)
        if (tareasHoy.isEmpty()) {
            val listaDefault = listOf(
                TaskEntity(descripcion = "Encender hornos de cocción", fecha = fechaHoy, rol = "Panadero"),
                TaskEntity(descripcion = "Calibrar molino de espresso", fecha = fechaHoy, rol = "Barista"),
                TaskEntity(descripcion = "Limpiar lanzas de vapor de cafetera", fecha = fechaHoy, rol = "Barista"),
                TaskEntity(descripcion = "Arqueo de caja inicial", fecha = fechaHoy, rol = "Cajero")
            )
            for (task in listaDefault) {
                val insertedId = taskDao.insertTask(task).toInt()
                programarAlarmaTarea(insertedId, task.descripcion)

                // Encolar pendiente de sincronización
                syncPendingDao.insertPendingOperation(
                    SyncPendingEntity(
                        taskId = insertedId,
                        operacion = "INSERT",
                        descripcion = task.descripcion,
                        fecha = fechaHoy,
                        rol = task.rol,
                        asignadoAId = null,
                        asignadoANombre = null,
                        fechaLimite = null
                    )
                )
            }
            programarSincronizacion()
        }
        sharedPrefs.edit().putString("PREF_LAST_RECURRENT_GENERATION_DATE", fechaHoy).apply()
    }

    private fun programarAlarmaTarea(id: Int, descripcion: String) {
        try {
            val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = android.content.Intent(context, com.example.cafeypan.receiver.AlertaTareaReceiver::class.java).apply {
                putExtra("idTarea", id)
                putExtra("nombreTarea", descripcion)
            }
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                id,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + (10 * 60 * 1000L)
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelarAlarmaTarea(id: Int) {
        try {
            val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = android.content.Intent(context, com.example.cafeypan.receiver.AlertaTareaReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                id,
                intent,
                android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
