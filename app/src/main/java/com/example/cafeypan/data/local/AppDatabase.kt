package com.example.cafeypan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.cafeypan.data.local.dao.TaskDao
import com.example.cafeypan.data.local.dao.UserDao
import com.example.cafeypan.data.local.dao.SyncPendingDao
import com.example.cafeypan.data.local.dao.WasteDao
import com.example.cafeypan.data.local.entity.TaskEntity
import com.example.cafeypan.data.local.entity.UserEntity
import com.example.cafeypan.data.local.entity.SyncPendingEntity
import com.example.cafeypan.data.local.entity.WasteEntity

@Database(entities = [TaskEntity::class, UserEntity::class, SyncPendingEntity::class, WasteEntity::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun userDao(): UserDao
    abstract fun syncPendingDao(): SyncPendingDao
    abstract fun wasteDao(): WasteDao
}
