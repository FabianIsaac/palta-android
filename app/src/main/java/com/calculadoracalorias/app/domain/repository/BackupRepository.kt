package com.calculadoracalorias.app.domain.repository

import com.calculadoracalorias.app.domain.model.backup.BackupDataPayload
import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    suspend fun exportBackupPayload(): BackupDataPayload
    suspend fun importBackupPayload(payload: BackupDataPayload)
    fun getLastBackupTimestamp(): Flow<Long?>
    suspend fun setLastBackupTimestamp(timestamp: Long)
}
