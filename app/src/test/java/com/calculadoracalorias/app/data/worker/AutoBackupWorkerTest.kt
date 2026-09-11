package com.calculadoracalorias.app.data.worker

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.calculadoracalorias.app.data.preferences.UserPreferences
import com.calculadoracalorias.app.data.preferences.UserPreferencesRepository
import com.calculadoracalorias.app.domain.model.backup.BackupDataPayload
import com.calculadoracalorias.app.domain.model.backup.BackupPreferences
import com.calculadoracalorias.app.domain.usecase.backup.ExportBackupUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class AutoBackupWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val workerParams: WorkerParameters = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val exportBackupUseCase: ExportBackupUseCase = mockk(relaxed = true)
    private val documentFolder: DocumentFile = mockk(relaxed = true)
    private val documentFile: DocumentFile = mockk(relaxed = true)

    private val samplePayload = BackupDataPayload(
        version = 1,
        exportedAt = 1725800000000L,
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
        every { context.applicationContext } returns context
    }

    @Test
    @DisplayName("doWork retorna success si el respaldo automático está deshabilitado")
    fun testDoWorkWhenDisabled() = runBlocking {
        every { userPreferencesRepository.userPreferencesFlow } returns flowOf(
            UserPreferences(autoBackupEnabled = false)
        )

        val worker = AutoBackupWorker(
            context = context,
            workerParams = workerParams,
            exportBackupUseCaseProvider = { exportBackupUseCase },
            userPreferencesRepositoryProvider = { userPreferencesRepository }
        )

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { exportBackupUseCase() }
    }

    @Test
    @DisplayName("doWork retorna retry si la carpeta no existe o no tiene permisos de escritura")
    fun testDoWorkWhenFolderNotWritable() = runBlocking {
        val folderUri = mockk<Uri>()
        every { folderUri.toString() } returns "content://mock/tree"
        every { userPreferencesRepository.userPreferencesFlow } returns flowOf(
            UserPreferences(
                autoBackupEnabled = true,
                autoBackupFolderUri = "content://mock/tree"
            )
        )
        every { documentFolder.exists() } returns true
        every { documentFolder.canWrite() } returns false

        val worker = AutoBackupWorker(
            context = context,
            workerParams = workerParams,
            exportBackupUseCaseProvider = { exportBackupUseCase },
            userPreferencesRepositoryProvider = { userPreferencesRepository },
            documentFileTreeProvider = { _, _ -> documentFolder },
            uriParser = { folderUri }
        )

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    @DisplayName("doWork exporta datos, escribe archivo y actualiza timestamp exitosamente")
    fun testDoWorkSuccess() = runBlocking {
        val folderUri = mockk<Uri>()
        val fileUri = mockk<Uri>()
        val outputStream = ByteArrayOutputStream()

        every { userPreferencesRepository.userPreferencesFlow } returns flowOf(
            UserPreferences(
                autoBackupEnabled = true,
                autoBackupFolderUri = "content://mock/tree"
            )
        )
        every { documentFolder.exists() } returns true
        every { documentFolder.canWrite() } returns true
        every { documentFolder.findFile(AutoBackupWorker.BACKUP_FILE_NAME) } returns null
        every { documentFolder.createFile(AutoBackupWorker.MIME_TYPE_JSON, AutoBackupWorker.BACKUP_FILE_NAME) } returns documentFile
        every { documentFile.uri } returns fileUri

        coEvery { exportBackupUseCase() } returns samplePayload

        val worker = AutoBackupWorker(
            context = context,
            workerParams = workerParams,
            exportBackupUseCaseProvider = { exportBackupUseCase },
            userPreferencesRepositoryProvider = { userPreferencesRepository },
            documentFileTreeProvider = { _, _ -> documentFolder },
            openOutputStreamProvider = { outputStream },
            uriParser = { folderUri }
        )

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)

        val writtenContent = outputStream.toString(Charsets.UTF_8.name())
        assertTrue(writtenContent.contains("\"exportedAt\": 1725800000000"))
        assertTrue(writtenContent.contains("\"appVersionName\": \"1.1\""))

        coVerify(exactly = 1) { userPreferencesRepository.setLastAutoBackupTimestamp(samplePayload.exportedAt) }
    }

    @Test
    @DisplayName("doWork retorna failure ante SecurityException")
    fun testDoWorkSecurityException() = runBlocking {
        every { userPreferencesRepository.userPreferencesFlow } returns flowOf(
            UserPreferences(
                autoBackupEnabled = true,
                autoBackupFolderUri = "content://mock/tree"
            )
        )
        val worker = AutoBackupWorker(
            context = context,
            workerParams = workerParams,
            exportBackupUseCaseProvider = { exportBackupUseCase },
            userPreferencesRepositoryProvider = { userPreferencesRepository },
            documentFileTreeProvider = { _, _ -> throw SecurityException("Permission revoked") }
        )

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    @DisplayName("AutoBackupWorker posee constructor binario (Context, WorkerParameters) compatible con WorkManager")
    fun testBinaryConstructorReflection() {
        val constructor = AutoBackupWorker::class.java.getConstructor(Context::class.java, WorkerParameters::class.java)
        org.junit.jupiter.api.Assertions.assertNotNull(constructor)
        val instance = constructor.newInstance(context, workerParams)
        org.junit.jupiter.api.Assertions.assertNotNull(instance)
    }
}
