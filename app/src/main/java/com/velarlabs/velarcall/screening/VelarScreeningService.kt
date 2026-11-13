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
import com.velarlabs.velarcall.ui.TAB_HISTORY

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
        val verifyStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            details.callerNumberVerificationStatus else -1
        val spoofed = verifyStatus == Connection.VERIFICATION_STATUS_FAILED

        val decider = CallDecider(this)
        val decision = try {
            decider.decide(raw, spoofed, checkRepeat = true)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao decidir; permitindo a chamada", e)
            Decision.Allow("Erro")
        }
        val settings = Settings(this)
        val isBlock = decision is Decision.Block
        // Modo suave: a chamada toca, mas é marcada como "teria bloqueado".
        val soft = isBlock && settings.softMode
        // Silenciar (em vez de rejeitar) deixa a chamada cair no correio de voz da operadora.
        val toVoicemail = isBlock && !soft && settings.sendBlockedToVoicemail
        val detail = recordDiagnostic(details, raw, verifyStatus, decider, decision, toVoicemail, soft)

        val response = when {
            decision is Decision.Allow || soft -> CallResponse.Builder().build() // deixa tocar
            toVoicemail -> CallResponse.Builder()
                .setSilenceCall(true)   // não toca; segue para o correio de voz se ninguém atender
                .setSkipNotification(true)
                .build()
            else -> CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()
        }
        respondToCall(details, response)

        if (isBlock) {
            val shown = raw.orEmpty()
            val reason = (decision as Decision.Block).reason + if (soft) " · modo suave (só marcado)" else ""
            VelarDb.get(this).logBlocked(shown, reason, toVoicemail, detail)
            notifyBlocked(shown, reason, toVoicemail, soft)
        }
    }

    /** Guarda um resumo da última chamada para a tela de diagnóstico ajudar a entender bloqueios indevidos. */
    private fun recordDiagnostic(
        details: Call.Details,
        raw: String?,
        verifyStatus: Int,
        decider: CallDecider,
        decision: Decision,
        toVoicemail: Boolean,
        soft: Boolean,
    ): String {
        val verify = when (verifyStatus) {
            Connection.VERIFICATION_STATUS_PASSED -> "verificado"
            Connection.VERIFICATION_STATUS_FAILED -> "falhou (suspeito de forjado)"
            Connection.VERIFICATION_STATUS_NOT_VERIFIED -> "não verificado"
            else -> "indisponível"
        }
        val result = when {
            decision is Decision.Block && soft -> "MARCADA (modo suave)"
            decision is Decision.Block && toVoicemail -> "CORREIO DE VOZ"
            decision is Decision.Block -> "BLOQUEADA"
            else -> "PERMITIDA"
        }
        val reason = when (decision) { is Decision.Block -> decision.reason; is Decision.Allow -> decision.reason }
        val count = decider.contactNumberCount()
        val contacts = if (count < 0) "sem permissão de agenda" else "$count números na agenda"
        val numberInfo = when {
            details.handle == null -> "número NÃO entregue pela operadora"
            raw.isNullOrBlank() -> "número vazio"
            else -> PhoneNumbers.pretty(raw)
        }
        val diag = buildString {
            append(result).append(" · ").append(reason).append('\n')
            append("Número: ").append(numberInfo).append('\n')
            append("Verificação da operadora: ").append(verify).append('\n')
            append("Agenda: ").append(contacts)
        }
        Settings(this).apply { lastCallDiag = diag; lastCallDiagAt = System.currentTimeMillis() }
        return diag
    }

    private fun notifyBlocked(raw: String, reason: String, toVoicemail: Boolean, soft: Boolean = false) {
        if (!Settings(this).notifyOnBlock) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, TAB_HISTORY),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(this, VelarCallApp.CHANNEL_BLOCKED)
            .setSmallIcon(R.drawable.ic_shield_v)
            .setColor(getColor(R.color.velar_brand))
            .setContentTitle(
                (when {
                    soft -> "Teria bloqueado: "
                    toVoicemail -> "Recado no correio de voz: "
                    else -> "Chamada bloqueada: "
                }) + PhoneNumbers.pretty(raw)
            )
            .setContentText(
                when {
                    soft -> "$reason · tocou (modo suave)"
                    toVoicemail -> "$reason · ouça no correio de voz"
                    else -> reason
                }
            )
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
