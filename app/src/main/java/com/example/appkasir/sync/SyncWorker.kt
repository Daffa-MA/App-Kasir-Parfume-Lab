package com.example.appkasir.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        // Sync Logic
        return try {
            SyncEngine.syncPendingTransactions(applicationContext)
            enqueue(applicationContext, 30)
            Result.success()
        } catch (_: Exception) {
            enqueue(applicationContext, 30)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "perfume_lab_sync_work"

        fun enqueue(context: Context, delaySeconds: Long) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun startNow(context: Context) {
            enqueue(context, 0)
        }
    }
}
