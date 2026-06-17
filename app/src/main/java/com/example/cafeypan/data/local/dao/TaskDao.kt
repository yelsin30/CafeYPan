package com.example.cafeypan.data.local.dao

import androidx.room.*
import com.example.cafeypan.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tareas ORDER BY id DESC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tareas WHERE fecha = :date ORDER BY id DESC")
    fun getTasksByDateFlow(date: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity): Int

    @Query("DELETE FROM tareas WHERE id = :id")
    suspend fun deleteTaskById(id: Int): Int

    @Delete
    suspend fun deleteTask(task: TaskEntity): Int

    @Query("SELECT * FROM tareas")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tareas WHERE fecha = :date")
    suspend fun getTasksByDate(date: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>): List<Long>

    @Query("DELETE FROM tareas WHERE id NOT IN (:keepIds)")
    suspend fun clearAllTasksExcept(keepIds: List<Int>): Int

    @Query("DELETE FROM tareas WHERE fecha = :date AND id NOT IN (:keepIds)")
    suspend fun clearTasksForDateExcept(date: String, keepIds: List<Int>): Int
}
