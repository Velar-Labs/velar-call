package com.velarlabs.velarcall.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.velarlabs.velarcall.R
import com.velarlabs.velarcall.data.VelarDb
import com.velarlabs.velarcall.ui.MainActivity
import java.util.Calendar

/** Widget na tela inicial: mostra quantas chamadas foram bloqueadas hoje. */
class VelarWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
    }

    companion object {
        /** Atualiza todos os widgets (chamado após um bloqueio). */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VelarWidget::class.java))
            ids.forEach { render(context, manager, it) }
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val today = VelarDb.get(context).blockedSince(startOfToday())
            val views = RemoteViews(context.packageName, R.layout.widget_velar)
            views.setTextViewText(R.id.widget_count, today.toString())
            views.setTextViewText(R.id.widget_label, if (today == 1) "bloqueada hoje" else "bloqueadas hoje")
            val pi = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            manager.updateAppWidget(id, views)
        }

        private fun startOfToday(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
