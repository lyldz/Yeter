package com.yeter.app

import android.app.Application

class YeterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            DailyWorker.schedule(this)
        } catch (e: Exception) {
            // Zamanlama başarısız olursa uygulama yine de çalışır
        }
    }
}
