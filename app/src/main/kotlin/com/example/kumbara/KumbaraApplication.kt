package com.example.kumbara

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class KumbaraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Room database singleton if needed (lazy init in other classes)
        // Schedule daily reminder at 8 AM
        val workRequest = PeriodicWorkRequestBuilder<com.example.kumbara.worker.ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "kumbara_daily_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun calculateInitialDelay(): Long {
        // Simple implementation: start after 1 minute for demo purposes.
        // In a real app you would compute time until next 08:00.
        return 60_000L
    }
}
