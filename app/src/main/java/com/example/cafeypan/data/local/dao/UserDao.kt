package com.example.cafeypan.data.local.dao

import androidx.room.*
import com.example.cafeypan.data.local.entity.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM usuarios WHERE pin = :pin LIMIT 1")
    suspend fun getUserByPin(pin: String): UserEntity?

    @Query("SELECT * FROM usuarios")
    suspend fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity): Int

    @Delete
    suspend fun deleteUser(user: UserEntity): Int

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun getUserCount(): Int
}
