package com.example.cafeypan.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cafeypan.data.local.AppDatabase
import com.example.cafeypan.data.local.dao.TaskDao
import com.example.cafeypan.data.local.dao.UserDao
import com.example.cafeypan.data.local.dao.SyncPendingDao
import com.example.cafeypan.data.local.dao.WasteDao
import com.example.cafeypan.data.local.entity.UserEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "cafeypan_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideSyncPendingDao(database: AppDatabase): SyncPendingDao {
        return database.syncPendingDao()
    }

    @Provides
    fun provideWasteDao(database: AppDatabase): WasteDao {
        return database.wasteDao()
    }
}
