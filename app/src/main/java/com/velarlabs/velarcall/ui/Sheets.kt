package com.velarlabs.velarcall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.velarlabs.velarcall.data.CallDecider
import com.velarlabs.velarcall.data.Decision
import com.velarlabs.velarcall.data.ListType
import com.velarlabs.velarcall.data.PhoneNumbers
import com.velarlabs.velarcall.data.VelarDb

/** Status manual de um número nas listas do app. */
enum class ManualStatus { NONE, ALLOWED, BLOCKED }

fun manualStatus(db: VelarDb, key: String): ManualStatus {
    val e = db.find(key)
    return when {
        e.any { it.type == ListType.BLOCK } -> ManualStatus.BLOCKED
        e.any { it.type == ListType.ALLOW } -> ManualStatus.ALLOWED
        else -> ManualStatus.NONE
    }
}

fun setManual(db: VelarDb, key: String, status: ManualStatus, label: String = "") {
    if (key.isEmpty()) return
    when (status) {
        ManualStatus.ALLOWED -> db.put(key, ListType.ALLOW, label)
        ManualStatus.BLOCKED -> db.put(key, ListType.BLOCK, label)
        ManualStatus.NONE -> { db.remove(key, ListType.ALLOW); db.remove(key, ListType.BLOCK) }
    }
}

/** Folha inferior com as ações para um número: bloquear, permitir ou tirar das listas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberSheet(
    raw: String,
    title: String? = null,
    label: String = "",
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    val db = remember { VelarDb.get(context) }
    val decider = remember { CallDecider(context) }
    val key = PhoneNumbers.key(raw)
    var status by remember(key) { mutableStateOf(manualStatus(db, key)) }
    val decision = remember(key, status) { decider.decide(raw, ignoreMasterSwitch = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(title ?: PhoneNumbers.pretty(raw), size = 52.dp)
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(title ?: PhoneNumbers.pretty(raw), style = MaterialTheme.typography.titleLarge)
                    if (title != null) Text(PhoneNumbers.pretty(raw), color = palette.muted)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (key.isEmpty()) {
                Text("Número oculto não pode ser adicionado às listas.", color = palette.muted)
            } else {
                DecisionBanner(decision)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (status != ManualStatus.BLOCKED) Button(
                        onClick = { setManual(db, key, ManualStatus.BLOCKED, label); status = ManualStatus.BLOCKED; onChanged() },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.danger, contentColor = androidx.compose.ui.graphics.Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text("Bloquear") }
                    if (status != ManualStatus.ALLOWED) Button(
                        onClick = { setManual(db, key, ManualStatus.ALLOWED, label); status = ManualStatus.ALLOWED; onChanged() },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.success, contentColor = androidx.compose.ui.graphics.Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text("Permitir") }
                }
                if (status != ManualStatus.NONE) {
                    TextButton(
                        onClick = { setManual(db, key, ManualStatus.NONE); status = ManualStatus.NONE; onChanged() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (status == ManualStatus.BLOCKED) "Desbloquear" else "Tirar da lista permitida") }
                }
            }
        }
    }
}

@Composable
fun DecisionBanner(decision: Decision) {
    val blocked = decision is Decision.Block
    val reason = when (decision) { is Decision.Block -> decision.reason; is Decision.Allow -> decision.reason }
    Panel {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (blocked) Icons.Filled.Warning else Icons.Filled.CheckCircle, null,
                tint = if (blocked) palette.danger else palette.success, modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    if (blocked) "Esse número seria bloqueado" else "Esse número tocaria normalmente",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(reason, style = MaterialTheme.typography.bodySmall, color = palette.muted)
            }
        }
    }
}

/** Folha para consultar um número digitado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LookupSheet(onDismiss: () -> Unit, onOpenNumber: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val valid = PhoneNumbers.key(input).length >= 8
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("Consultar número", style = MaterialTheme.typography.titleLarge)
            Text("Veja se ele tocaria ou seria bloqueado.", color = palette.muted)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = input, onValueChange = { input = it }, singleLine = true,
                label = { Text("Número com DDD") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = valid, onClick = { onOpenNumber(input) },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Consultar") }
        }
    }
}

/** Diálogo para adicionar um número digitado a uma lista. */
@Composable
fun AddNumberDialog(title: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var num by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val valid = PhoneNumbers.key(num).length >= 8
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = num, onValueChange = { num = it }, singleLine = true,
                    label = { Text("Número com DDD") }, shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = label, onValueChange = { label = it.take(40) }, singleLine = true,
                    label = { Text("Nome ou motivo (opcional)") }, shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(num, label.trim()) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
