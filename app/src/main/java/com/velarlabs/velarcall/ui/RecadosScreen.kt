package com.velarlabs.velarcall.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.velarlabs.velarcall.data.BlockedCall
import com.velarlabs.velarcall.data.PhoneNumbers
import com.velarlabs.velarcall.data.Settings
import com.velarlabs.velarcall.data.VelarDb

@Composable
fun RecadosScreen(tick: Int, bump: () -> Unit) {
    val context = LocalContext.current
    val db = remember { VelarDb.get(context) }
    val settings = remember { Settings(context) }
    val calls = remember(tick) { db.blocked(500) }
    val groups = remember(calls) { calls.groupBy { dayLabel(it.at) } }
    val toVoicemail = remember(tick) { settings.sendBlockedToVoicemail }
    var selected by remember { mutableStateOf<BlockedCall?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = ScreenPadding) {
        // ---------- cabeçalho: o que fazer com a chamada bloqueada
        item {
            Panel {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(voicemailPainter(), null, tint = palette.brand, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.size(12.dp))
                        Text("Ao bloquear uma chamada", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = toVoicemail,
                            onClick = { settings.sendBlockedToVoicemail = true; bump() },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Correio de voz") }
                        SegmentedButton(
                            selected = !toVoicemail,
                            onClick = { settings.sendBlockedToVoicemail = false; bump() },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Rejeitar") }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (toVoicemail)
                            "Silencia a chamada e deixa cair no correio de voz da sua operadora. Quem ligou pode deixar um recado, e você ouve e retorna por aqui."
                        else
                            "Recusa a chamada na hora, como ao declinar uma ligação. Na maioria das operadoras ainda cai no correio de voz, mais rápido.",
                        style = MaterialTheme.typography.bodyMedium, color = palette.muted
                    )
                    if (toVoicemail) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { openVoicemail(context) }, shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Ouvir correio de voz")
                        }
                    }
                }
            }
        }

        // ---------- lista
        if (calls.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nenhuma chamada ainda", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "As chamadas tratadas pelo VelarCall aparecem aqui.",
                        style = MaterialTheme.typography.bodyMedium, color = palette.muted
                    )
                }
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${calls.size} chamadas", style = MaterialTheme.typography.bodyMedium,
                        color = palette.muted, modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    TextButton(onClick = { confirmClear = true }) { Text("Limpar") }
                }
            }
            groups.forEach { (day, list) ->
                item(key = "h-$day") { SectionLabel(day.uppercase()) }
                item(key = "p-$day") {
                    Panel {
                        list.forEachIndexed { i, c ->
                            if (i > 0) RowDivider()
                            val canCall = PhoneNumbers.key(c.number).isNotEmpty()
                            ListRow(
                                title = PhoneNumbers.pretty(c.number),
                                subtitle = if (c.toVoicemail) "${c.reason} · correio de voz" else c.reason,
                                leading = {
                                    Avatar("x", tint = if (c.toVoicemail) palette.brand else palette.danger) {
                                        if (c.toVoicemail) Icon(voicemailPainter(), null, tint = palette.brand, modifier = Modifier.size(20.dp))
                                        else Icon(Icons.Filled.Call, null, tint = palette.danger, modifier = Modifier.size(20.dp))
                                    }
                                },
                                trailing = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(timeLabel(c.at), style = MaterialTheme.typography.bodySmall, color = palette.muted)
                                        if (canCall) {
                                            Spacer(Modifier.size(4.dp))
                                            IconButton(onClick = { callNumber(context, c.number) }) {
                                                Icon(Icons.Filled.Call, "Ligar de volta", tint = palette.success, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                },
                                onClick = { selected = c }
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "Toque num número para liberar ou bloquear sempre. Toque no telefone para ligar de volta.",
                    style = MaterialTheme.typography.bodySmall, color = palette.muted,
                    modifier = Modifier.padding(PaddingValues(start = 4.dp, top = 16.dp))
                )
            }
        }
    }

    selected?.let { c -> CallDetailSheet(call = c, onDismiss = { selected = null }, onChanged = bump) }
    if (confirmClear) ConfirmDialog(
        title = "Limpar lista?",
        text = "Os registros de chamadas serão apagados. Suas listas e o correio de voz da operadora não mudam.",
        confirm = "Limpar",
        onConfirm = { db.clearHistory(); confirmClear = false; bump() },
        onDismiss = { confirmClear = false }
    )
}

/** Detalhe de uma chamada: motivo, diagnóstico e ações. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun CallDetailSheet(call: BlockedCall, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { VelarDb.get(context) }
    val key = PhoneNumbers.key(call.number)
    var status by remember(key) { mutableStateOf(manualStatus(db, key)) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surface,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(PhoneNumbers.pretty(call.number), size = 52.dp) {
                    if (call.toVoicemail) Icon(voicemailPainter(), null, tint = palette.brand, modifier = Modifier.size(22.dp))
                    else Icon(Icons.Filled.Call, null, tint = palette.danger, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(PhoneNumbers.pretty(call.number), style = MaterialTheme.typography.titleLarge)
                    Text(
                        (if (call.toVoicemail) "${call.reason} · correio de voz" else call.reason) +
                            "  ·  ${dayLabel(call.at)} ${timeLabel(call.at)}",
                        style = MaterialTheme.typography.bodySmall, color = palette.muted
                    )
                }
            }
            if (call.detail.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Panel {
                    Text(
                        call.detail, style = MaterialTheme.typography.bodyMedium, color = palette.text,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (key.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { callNumber(context, call.number) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Call, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Ligar de volta")
                    }
                    if (status == ManualStatus.BLOCKED) {
                        Button(
                            onClick = { setManual(db, key, ManualStatus.NONE); status = ManualStatus.NONE; onChanged() },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.success, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Permitir sempre") }
                    } else {
                        Button(
                            onClick = { setManual(db, key, ManualStatus.BLOCKED); status = ManualStatus.BLOCKED; onChanged() },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.danger, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Bloquear sempre") }
                    }
                }
            }
        }
    }
}


/** Abre o discador na ligação de volta (ACTION_DIAL não precisa de permissão). */
fun callNumber(context: Context, rawNumber: String) {
    val digits = PhoneNumbers.digits(rawNumber)
    if (digits.isEmpty()) return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Liga para o correio de voz da operadora. */
fun openVoicemail(context: Context) {
    val tries = listOf(
        Intent(Intent.ACTION_DIAL, Uri.fromParts("voicemail", "", null)),
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:")),
    )
    for (intent in tries) {
        if (runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)) return
    }
}

