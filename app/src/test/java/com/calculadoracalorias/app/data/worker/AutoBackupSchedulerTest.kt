package com.calculadoracalorias.app.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AutoBackupSchedulerTest {

    private val context: Context = mockk(relaxed = true)
    private val workManager: WorkManager = mockk(relaxed = true)
    private lateinit var scheduler: AutoBackupScheduler

    @BeforeEach
    fun setUp() {
        scheduler = AutoBackupScheduler(
            context = context,
            workManagerProvider = { workManager }
        )
    }

    @Test
    @DisplayName("scheduleDailyBackup debe encolar trabajo periódico con UPDATE")
    fun testScheduleDailyBackup() {
        every {
            workManager.enqueueUniquePeriodicWork(
                AutoBackupScheduler.PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                any<PeriodicWorkRequest>()
            )
        } returns mockk(relaxed = true)

        scheduler.scheduleDailyBackup()

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                AutoBackupScheduler.PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                any<PeriodicWorkRequest>()
            )
        }
    }

    @Test
    @DisplayName("cancelAutoBackup debe cancelar el trabajo periódico y el inmediato")
    fun testCancelAutoBackup() {
        every { workManager.cancelUniqueWork(any()) } returns mockk(relaxed = true)

        scheduler.cancelAutoBackup()

        verify(exactly = 1) { workManager.cancelUniqueWork(AutoBackupScheduler.PERIODIC_WORK_NAME) }
        verify(exactly = 1) { workManager.cancelUniqueWork(AutoBackupScheduler.IMMEDIATE_WORK_NAME) }
    }

    @Test
    @DisplayName("triggerImmediateBackup debe encolar trabajo OneTime con REPLACE")
    fun testTriggerImmediateBackup() {
        every {
            workManager.enqueueUniqueWork(
                AutoBackupScheduler.IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        } returns mockk(relaxed = true)

        scheduler.triggerImmediateBackup()

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                AutoBackupScheduler.IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
    }
}
