package com.yeter.app

import android.app.Application

class YeterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val hour = getSharedPreferences("yeter_app_prefs", MODE_PRIVATE).getInt("notify_hour", 9)
            DailyWorker.schedule(this, hour)
            WidgetWorker.schedule(this)
        } catch (e: Exception) {
            // Zamanlama başarısız olursa uygulama yine de çalışır
        }
    }
}
