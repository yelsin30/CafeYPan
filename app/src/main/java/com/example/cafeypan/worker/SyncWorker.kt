package com.example.cafeypan.worker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cafeypan.data.ApiService
import com.example.cafeypan.data.local.dao.SyncPendingDao
import com.example.cafeypan.data.local.dao.TaskDao
import com.example.cafeypan.data.local.dao.WasteDao
import com.example.cafeypan.model.RespuestaBasica
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import retrofit2.Response

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun apiService(): ApiService
        fun syncPendingDao(): SyncPendingDao
        fun taskDao(): TaskDao
        fun wasteDao(): WasteDao
    }

    override suspend fun doWork(): Result {
        val appContext = applicationContext

        // Green Computing: battery check
        val batteryStatusIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = (level / scale.toFloat()) * 100

        val status = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        if (batteryPct in 0f..15f && !isCharging) {
            // Delay synchronization if battery is critical and not charging
            return Result.retry()
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            SyncWorkerEntryPoint::class.java
        )
        
        val apiService = entryPoint.apiService()
        val syncPendingDao = entryPoint.syncPendingDao()
        val wasteDao = entryPoint.wasteDao()

        // 1. Sync tasks operations
        val pendingOperations = syncPendingDao.getAllPendingOperations()
        for (op in pendingOperations) {
            try {
                val response: Response<RespuestaBasica> = when (op.operacion) {
                    "INSERT" -> {
                        apiService.guardarTarea(
                            descripcion = op.descripcion ?: "",
                            fecha = op.fecha ?: "",
                            rol = op.rol ?: "",
                            asignadoAId = op.asignadoAId,
                            asignadoANombre = op.asignadoANombre,
                            fechaLimite = op.fechaLimite,
                            duracionMinutos = op.durationMinutes,
                            notas = op.notas
                        )
                    }
                    "COMPLETE" -> {
                        apiService.completarTarea(
                            id = op.taskId,
                            completadoPorNombre = op.completadoPorNombre
                        )
                    }
                    "EDIT" -> {
                        apiService.editarTarea(
                            id = op.taskId,
                            descripcion = op.descripcion ?: "",
                            rol = op.rol,
                            asignadoAId = op.asignadoAId,
                            asignadoANombre = op.asignadoANombre,
                            fecha = op.fecha,
                            fechaLimite = op.fechaLimite,
                            duracionMinutos = op.durationMinutes,
                            notas = op.notas
                        )
                    }
                    "DELETE" -> {
                        apiService.eliminarTarea(op.taskId)
                    }
                    else -> continue
                }

                val isSuccessful = response.isSuccessful
                val body = response.body()
                val success = isSuccessful && body?.exito == true

                if (success) {
                    syncPendingDao.deletePendingOperation(op.syncId)
                } else {
                    val code = response.code()
                    if (code in 400..499) {
                        syncPendingDao.deletePendingOperation(op.syncId)
                    } else {
                        return Result.retry()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return Result.retry()
            }
        }

        // 2. Sync waste items
        val pendingWastes = wasteDao.getUnsyncedWastes()
        for (waste in pendingWastes) {
            try {
                val response = apiService.guardarMerma(
                    product = waste.product,
                    quantity = waste.quantity,
                    reason = waste.reason,
                    cost = waste.cost,
                    date = waste.date
                )
                if (response.isSuccessful && response.body()?.exito == true) {
                    wasteDao.updateWaste(waste.copy(isSynced = true))
                } else {
                    val code = response.code()
                    if (code in 400..499) {
                        // Client error, discard
                        wasteDao.updateWaste(waste.copy(isSynced = true))
                    } else {
                        return Result.retry()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return Result.retry()
            }
        }

        return Result.success()
    }
}
