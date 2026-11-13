package com.velarlabs.velarcall.screening

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.velarlabs.velarcall.R

/**
 * Mostra um aviso flutuante sobre a tela da chamada, identificando o número.
 * Precisa da permissão "sobrepor a outros apps" (SYSTEM_ALERT_WINDOW).
 */
object CallOverlay {

    private var view: View? = null
    private val handler = Handler(Looper.getMainLooper())

    /** level: 0 contato (verde), 1 desconhecido (âmbar), 2 provável spam (vermelho). */
    fun show(context: Context, number: String, label: String, level: Int) {
        val app = context.applicationContext
        if (!Settings.canDrawOverlays(app)) return
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        remove(app)
        try {
            val v = LayoutInflater.from(app).inflate(R.layout.overlay_caller, null)
            v.findViewById<TextView>(R.id.overlay_number).text = number
            v.findViewById<TextView>(R.id.overlay_label).apply {
                text = label
                setTextColor(colorFor(level))
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
                y = (72 * app.resources.displayMetrics.density).toInt()
            }
            v.setOnClickListener { remove(app) }
            wm.addView(v, params)
            view = v
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ remove(app) }, 15_000)
        } catch (e: Exception) {
            // Em alguns aparelhos o overlay pode falhar; não deve derrubar a triagem.
        }
    }

    fun remove(context: Context) {
        val v = view ?: return
        view = null
        try {
            (context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.removeView(v)
        } catch (e: Exception) {
        }
    }

    private fun colorFor(level: Int): Int = when (level) {
        0 -> Color.parseColor("#12A150")
        2 -> Color.parseColor("#E5484D")
        else -> Color.parseColor("#D97706")
    }
}
