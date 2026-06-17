package com.example.cafeypan.data.local.dao

import androidx.room.*
import com.example.cafeypan.data.local.entity.WasteEntity

@Dao
interface WasteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaste(waste: WasteEntity): Long

    @Query("SELECT * FROM wastes ORDER BY id DESC")
    suspend fun getAllWastes(): List<WasteEntity>

    @Query("SELECT * FROM wastes WHERE isSynced = 0")
    suspend fun getUnsyncedWastes(): List<WasteEntity>

    @Update
    suspend fun updateWaste(waste: WasteEntity): Int

    @Query("DELETE FROM wastes")
    suspend fun clearAllWastes(): Int
}
