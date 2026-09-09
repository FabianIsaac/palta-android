package com.calculadoracalorias.app.domain.usecase.backup

import com.calculadoracalorias.app.domain.model.backup.BackupDataPayload
import com.calculadoracalorias.app.domain.repository.BackupRepository

class ImportBackupUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(payload: BackupDataPayload) {
        require(payload.version >= 1) { "Versión de respaldo no compatible: ${payload.version}" }
        backupRepository.importBackupPayload(payload)
        backupRepository.setLastBackupTimestamp(System.currentTimeMillis())
    }
}
