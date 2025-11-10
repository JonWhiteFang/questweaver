package dev.questweaver.sync.firebase

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import java.util.concurrent.TimeUnit

class BackupWorker(appCtx: Context, params: WorkerParameters) : CoroutineWorker(appCtx, params) {
    override suspend fun doWork(): Result {
        // Stub implementation - will upload event deltas to cloud storage
        return Result.success()
    }
}

fun scheduleBackups(ctx: Context) {
    WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
        "backup",
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.HOURS).build()
    )
}
