package com.velarlabs.velarcall

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.velarlabs.velarcall.sync.BlocklistSync

class VelarCallApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_BLOCKED, getString(R.string.channel_blocked), NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        BlocklistSync.schedule(this)
    }

    companion object {
        const val CHANNEL_BLOCKED = "blocked_calls"
    }
}
