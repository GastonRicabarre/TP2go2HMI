package com.example.go2hmi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMs: Long,
    val type: String,
    val detail: String
)
