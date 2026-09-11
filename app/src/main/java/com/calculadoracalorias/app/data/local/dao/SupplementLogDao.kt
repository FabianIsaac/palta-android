package com.calculadoracalorias.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calculadoracalorias.app.data.local.entity.SupplementLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementLogDao {

    @Query("SELECT * FROM supplement_logs WHERE date = :date")
    fun getLogsForDate(date: String): Flow<List<SupplementLogEntity>>

    @Query("SELECT * FROM supplement_logs WHERE date >= :startDate AND date <= :endDate")
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<SupplementLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SupplementLogEntity)

    @Query("DELETE FROM supplement_logs WHERE date = :date AND supplementId = :supplementId")
    suspend fun deleteLog(date: String, supplementId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM supplement_logs WHERE date = :date AND supplementId = :supplementId)")
    suspend fun isSupplementTaken(date: String, supplementId: String): Boolean

    @Query("SELECT * FROM supplement_logs ORDER BY takenTimestamp ASC")
    suspend fun getAllLogs(): List<SupplementLogEntity>

    @Query("DELETE FROM supplement_logs")
    suspend fun deleteAllLogs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<SupplementLogEntity>)

    @Query("SELECT supplementId, COUNT(*) as count FROM supplement_logs GROUP BY supplementId")
    fun getAllSupplementIntakeCounts(): Flow<List<SupplementIntakeCountTuple>>
}

data class SupplementIntakeCountTuple(
    val supplementId: String,
    val count: Int
)
