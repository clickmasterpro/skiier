package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rankings")
data class RankingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerName: String,
    val coins: Int,
    val duration: Int, // in seconds
    val timestamp: Long = System.currentTimeMillis()
)
