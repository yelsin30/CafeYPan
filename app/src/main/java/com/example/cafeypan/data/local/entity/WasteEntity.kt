package com.example.cafeypan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wastes")
data class WasteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val product: String,
    val quantity: Double,
    val reason: String,
    val cost: Double,
    val date: String,
    val isSynced: Boolean = false
)
