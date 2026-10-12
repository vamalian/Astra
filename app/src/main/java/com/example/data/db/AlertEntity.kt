package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guard_alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventType: String, // "TOUCH BREACH", "MOVEMENT DETECTED", "TEST ALERT"
    val detectionMode: String, // "TOUCH", "MOVEMENT", "BOTH"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val dismissed: Boolean = false
)
