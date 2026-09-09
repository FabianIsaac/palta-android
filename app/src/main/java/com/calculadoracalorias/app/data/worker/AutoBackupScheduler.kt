package com.calculadoracalorias.app.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AutoBackupScheduler(
    private val context: Context,
    private val workManagerProvider: () -> WorkManager = { WorkManager.getInstance(context) }
) {
    companion object {
        const val PERIODIC_WORK_NAME = AutoBackupWorker.WORK_NAME
        const val IMMEDIATE_WORK_NAME = "palta_immediate_auto_backup"
    }

    fun scheduleDailyBackup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        workManagerProvider().enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }

    fun cancelAutoBackup() {
        workManagerProvider().cancelUniqueWork(PERIODIC_WORK_NAME)
        workManagerProvider().cancelUniqueWork(IMMEDIATE_WORK_NAME)
    }

    fun triggerImmediateBackup() {
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setConstraints(constraints)
            .build()

        workManagerProvider().enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
    }
}
