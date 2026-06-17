package com.example.cafeypan.data.repository

import android.content.Context
import androidx.work.*
import com.example.cafeypan.data.ApiService
import com.example.cafeypan.data.local.dao.WasteDao
import com.example.cafeypan.data.local.entity.WasteEntity
import com.example.cafeypan.worker.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WasteRepository @Inject constructor(
    private val wasteDao: WasteDao,
    private val apiService: ApiService,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    suspend fun registrarMerma(
        product: String,
        quantity: Double,
        reason: String,
        cost: Double,
        date: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val localWaste = WasteEntity(
                product = product,
                quantity = quantity,
                reason = reason,
                cost = cost,
                date = date,
                isSynced = false
            )
            wasteDao.insertWaste(localWaste)
            programarSincronizacion()
            return@withContext Result.success(true)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun fetchCostoMermaSemanal(): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerMermaSemanal()
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!.totalCost)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Fallback local: calculate from Room for wastes in the last 7 days
        try {
            val hoy = java.util.Date()
            val haceSieteDias = java.util.Date(hoy.time - 7 * 24 * 60 * 60 * 1000L)
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val dateLimitStr = format.format(haceSieteDias)
            
            val allWastes = wasteDao.getAllWastes()
            val totalCost = allWastes.filter { it.date >= dateLimitStr }.sumOf { it.cost }
            return@withContext Result.success(totalCost)
        } catch (e: Exception) {
            return@withContext Result.success(0.0)
        }
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
}
