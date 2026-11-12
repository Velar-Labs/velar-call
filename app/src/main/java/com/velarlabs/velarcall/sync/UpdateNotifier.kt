package com.velarlabs.velarcall.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.velarlabs.velarcall.R
import com.velarlabs.velarcall.ui.MainActivity
import com.velarlabs.velarcall.ui.TAB_SETTINGS

object UpdateNotifier {
    const val CHANNEL = "updates"

    fun notify(context: Context, version: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, TAB_SETTINGS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_shield_v)
            .setColor(context.getColor(R.color.velar_brand))
            .setContentTitle("Atualização disponível: $version")
            .setContentText("Toque para atualizar o VelarCall.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(777, n)
    }
}
