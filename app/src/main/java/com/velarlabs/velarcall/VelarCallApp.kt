package com.velarlabs.velarcall

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.velarlabs.velarcall.sync.BlocklistSync
import com.velarlabs.velarcall.sync.UpdateNotifier
import com.velarlabs.velarcall.sync.Updater

class VelarCallApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_BLOCKED, getString(R.string.channel_blocked), NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(UpdateNotifier.CHANNEL, "Atualizações", NotificationManager.IMPORTANCE_DEFAULT)
        )
        BlocklistSync.schedule(this)
        Updater.schedule(this)
    }

    companion object {
        const val CHANNEL_BLOCKED = "blocked_calls"
    }
}
