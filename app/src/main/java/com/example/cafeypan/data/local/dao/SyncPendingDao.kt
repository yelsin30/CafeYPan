package com.example.cafeypan.data.local.dao

import androidx.room.*
import com.example.cafeypan.data.local.entity.SyncPendingEntity

@Dao
interface SyncPendingDao {
    @Query("SELECT * FROM sync_pending ORDER BY syncId ASC")
    suspend fun getAllPendingOperations(): List<SyncPendingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingOperation(operation: SyncPendingEntity): Long

    @Query("DELETE FROM sync_pending WHERE syncId = :syncId")
    suspend fun deletePendingOperation(syncId: Int): Int

    @Query("SELECT COUNT(*) FROM sync_pending")
    suspend fun getPendingCount(): Int
}
