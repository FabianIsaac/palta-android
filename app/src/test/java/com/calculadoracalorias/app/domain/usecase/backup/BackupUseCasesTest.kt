package com.calculadoracalorias.app.domain.usecase.backup

import com.calculadoracalorias.app.domain.model.backup.BackupDataPayload
import com.calculadoracalorias.app.domain.model.backup.BackupPreferences
import com.calculadoracalorias.app.domain.repository.BackupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BackupUseCasesTest {

    private val backupRepository: BackupRepository = mockk(relaxed = true)
    private lateinit var exportBackupUseCase: ExportBackupUseCase
    private lateinit var importBackupUseCase: ImportBackupUseCase

    private val samplePayload = BackupDataPayload(
        version = 1,
        exportedAt = 1788775200000L,
        appVersionName = "1.1",
        meals = emptyList(),
        supplements = emptyList(),
        supplementLogs = emptyList(),
        preferences = BackupPreferences(
            targetCalories = 2000.0,
            targetProteinGrams = 150.0,
            targetCarbsGrams = 200.0,
            targetFatGrams = 65.0
        )
    )

    @BeforeEach
    fun setUp() {
        exportBackupUseCase = ExportBackupUseCase(backupRepository)
        importBackupUseCase = ImportBackupUseCase(backupRepository)
    }

    @Test
    @DisplayName("ExportBackupUseCase debe obtener el payload del repositorio y registrar el timestamp")
    fun testExportBackupUseCase() = runBlocking {
        coEvery { backupRepository.exportBackupPayload() } returns samplePayload

        val result = exportBackupUseCase()

        assertEquals(samplePayload, result)
        coVerify(exactly = 1) { backupRepository.exportBackupPayload() }
        coVerify(exactly = 1) { backupRepository.setLastBackupTimestamp(samplePayload.exportedAt) }
    }

    @Test
    @DisplayName("ImportBackupUseCase debe invocar importBackupPayload y actualizar el timestamp")
    fun testImportBackupUseCaseSuccess() = runBlocking {
        importBackupUseCase(samplePayload)

        coVerify(exactly = 1) { backupRepository.importBackupPayload(samplePayload) }
        coVerify(exactly = 1) { backupRepository.setLastBackupTimestamp(any()) }
    }

    @Test
    @DisplayName("ImportBackupUseCase debe rechazar versiones incompatibles")
    fun testImportBackupUseCaseInvalidVersion() {
        val invalidPayload = samplePayload.copy(version = 0)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                importBackupUseCase(invalidPayload)
            }
        }
    }
}
