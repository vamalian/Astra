package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM guard_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Query("UPDATE guard_alerts SET dismissed = 1 WHERE dismissed = 0")
    suspend fun dismissAllAlerts()

    @Query("DELETE FROM guard_alerts")
    suspend fun clearAllAlerts()

    @Query("SELECT COUNT(*) FROM guard_alerts")
    fun getAlertCount(): Flow<Int>
}
