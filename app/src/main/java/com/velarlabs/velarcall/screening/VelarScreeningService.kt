package com.velarlabs.velarcall.screening

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.util.Log
import com.velarlabs.velarcall.R
import com.velarlabs.velarcall.VelarCallApp
import com.velarlabs.velarcall.data.CallDecider
import com.velarlabs.velarcall.data.Decision
import com.velarlabs.velarcall.data.PhoneNumbers
import com.velarlabs.velarcall.data.Settings
import com.velarlabs.velarcall.data.VelarDb
import com.velarlabs.velarcall.ui.MainActivity

/**
 * Chamado pelo Android a cada ligação recebida, quando o VelarCall é o
 * app de "identificação de chamadas e spam" (papel ROLE_CALL_SCREENING).
 * Precisa responder em poucos segundos, por isso tudo aqui é local.
 */
class VelarScreeningService : CallScreeningService() {

    override fun onScreenCall(details: Call.Details) {
        val incoming = details.callDirection == Call.Details.DIRECTION_INCOMING
        if (!incoming) {
            respondToCall(details, CallResponse.Builder().build())
            return
        }

        val raw = details.handle?.schemeSpecificPart
        val spoofed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            details.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED

        val decision = try {
            CallDecider(this).decide(raw, spoofed)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao decidir; permitindo a chamada", e)
            Decision.Allow("Erro")
        }

        val response = when (decision) {
            is Decision.Block -> CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()
            is Decision.Allow -> CallResponse.Builder().build()
        }
        respondToCall(details, response)

        if (decision is Decision.Block) {
            val shown = raw.orEmpty()
            VelarDb.get(this).logBlocked(shown, decision.reason)
            notifyBlocked(shown, decision.reason)
        }
    }

    private fun notifyBlocked(raw: String, reason: String) {
        if (!Settings(this).notifyOnBlock) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_HISTORY),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(this, VelarCallApp.CHANNEL_BLOCKED)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Chamada bloqueada: ${PhoneNumbers.pretty(raw)}")
            .setContentText(reason)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), n)
    }

    companion object {
        private const val TAG = "VelarScreening"
    }
}
