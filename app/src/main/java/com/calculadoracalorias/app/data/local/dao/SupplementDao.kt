package com.calculadoracalorias.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calculadoracalorias.app.data.local.entity.SupplementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementDao {
    @Query("SELECT * FROM supplements ORDER BY isCustom ASC, name ASC")
    fun getAllSupplements(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements WHERE isActive = 1 ORDER BY isCustom ASC, name ASC")
    fun getActiveSupplements(): Flow<List<SupplementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(supplement: SupplementEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(supplements: List<SupplementEntity>)

    @Query("UPDATE supplements SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: String, isActive: Boolean)

    @Query("DELETE FROM supplements WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM supplements ORDER BY isCustom ASC, name ASC")
    suspend fun getAllSupplementsList(): List<SupplementEntity>

    @Query("DELETE FROM supplements")
    suspend fun deleteAllSupplements()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(supplements: List<SupplementEntity>)
}
