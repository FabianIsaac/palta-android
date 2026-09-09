package com.calculadoracalorias.app.domain.usecase.backup

import com.calculadoracalorias.app.domain.model.backup.BackupDataPayload
import com.calculadoracalorias.app.domain.repository.BackupRepository

class ExportBackupUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): BackupDataPayload {
        val payload = backupRepository.exportBackupPayload()
        backupRepository.setLastBackupTimestamp(payload.exportedAt)
        return payload
    }
}
