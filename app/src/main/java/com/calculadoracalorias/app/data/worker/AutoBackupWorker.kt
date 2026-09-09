package com.calculadoracalorias.app.data.worker

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.calculadoracalorias.app.data.local.AppDatabase
import com.calculadoracalorias.app.data.preferences.UserPreferencesRepository
import com.calculadoracalorias.app.data.preferences.userDataStore
import com.calculadoracalorias.app.data.repository.RoomBackupRepository
import com.calculadoracalorias.app.domain.model.backup.BackupDataPayload
import com.calculadoracalorias.app.domain.usecase.backup.ExportBackupUseCase
import kotlinx.coroutines.flow.first
import java.io.OutputStream

class AutoBackupWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val exportBackupUseCaseProvider: () -> ExportBackupUseCase = {
        val database = AppDatabase.getDatabase(context)
        val userPrefs = UserPreferencesRepository(context.userDataStore)
        val backupRepo = RoomBackupRepository(
            appDatabase = database,
            mealDao = database.mealDao(),
            supplementDao = database.supplementDao(),
            supplementLogDao = database.supplementLogDao(),
            userPreferencesRepository = userPrefs
        )
        ExportBackupUseCase(backupRepo)
    },
    private val userPreferencesRepositoryProvider: () -> UserPreferencesRepository = {
        UserPreferencesRepository(context.userDataStore)
    },
    private val documentFileTreeProvider: (Context, Uri) -> DocumentFile? = { ctx, uri ->
        DocumentFile.fromTreeUri(ctx, uri)
    },
    private val openOutputStreamProvider: (Uri) -> OutputStream? = { uri ->
        context.contentResolver.openOutputStream(uri, "wt")
    },
    private val uriParser: (String) -> Uri = { Uri.parse(it) }
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "palta_auto_backup"
        const val BACKUP_FILE_NAME = "palta_respaldo_automatico.json"
        const val MIME_TYPE_JSON = "application/json"
    }

    override suspend fun doWork(): Result {
        return try {
            val userPrefsRepo = userPreferencesRepositoryProvider()
            val preferences = userPrefsRepo.userPreferencesFlow.first()

            if (!preferences.autoBackupEnabled || preferences.autoBackupFolderUri.isNullOrBlank()) {
                return Result.success()
            }

            val folderUri = try {
                uriParser(preferences.autoBackupFolderUri)
            } catch (e: Exception) {
                return Result.failure()
            }

            val folder = documentFileTreeProvider(applicationContext, folderUri)
            if (folder == null || !folder.exists() || !folder.canWrite()) {
                return Result.retry()
            }

            val backupFile = folder.findFile(BACKUP_FILE_NAME)
                ?: folder.createFile(MIME_TYPE_JSON, BACKUP_FILE_NAME)
                ?: return Result.retry()

            val exportBackupUseCase = exportBackupUseCaseProvider()
            val payload = exportBackupUseCase()
            val jsonString = BackupDataPayload.toJson(payload)

            val outputStream = openOutputStreamProvider(backupFile.uri) ?: return Result.retry()
            outputStream.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
            }

            userPrefsRepo.setLastAutoBackupTimestamp(payload.exportedAt)
            Result.success()
        } catch (e: SecurityException) {
            Result.failure()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
