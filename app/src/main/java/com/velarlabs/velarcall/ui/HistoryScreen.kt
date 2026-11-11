package com.velarlabs.velarcall.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.velarlabs.velarcall.data.PhoneNumbers
import com.velarlabs.velarcall.data.VelarDb

@Composable
fun HistoryScreen(tick: Int, bump: () -> Unit) {
    val context = LocalContext.current
    val db = remember { VelarDb.get(context) }
    val calls = remember(tick) { db.blocked(500) }
    val groups = remember(calls) { calls.groupBy { dayLabel(it.at) } }
    var selected by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    if (calls.isEmpty()) {
        EmptyState(Icons.Filled.Call, "Nenhuma chamada bloqueada", "As chamadas bloqueadas pelo VelarCall aparecem aqui.")
        return
    }

    LazyColumn(contentPadding = ScreenPadding) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${calls.size} chamadas bloqueadas", style = MaterialTheme.typography.bodyMedium,
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
                        ListRow(
                            title = PhoneNumbers.pretty(c.number),
                            subtitle = c.reason,
                            leading = {
                                Avatar("x", tint = palette.danger) {
                                    Icon(Icons.Filled.Call, null, tint = palette.danger, modifier = Modifier.size(20.dp))
                                }
                            },
                            trailing = {
                                Text(timeLabel(c.at), style = MaterialTheme.typography.bodySmall, color = palette.muted)
                            },
                            onClick = { selected = c.number }
                        )
                    }
                }
            }
        }
        item { Text(
            "Toque em um número para liberar ou bloquear sempre.",
            style = MaterialTheme.typography.bodySmall, color = palette.muted,
            modifier = Modifier.padding(PaddingValues(start = 4.dp, top = 16.dp))
        ) }
    }

    selected?.let { n -> NumberSheet(raw = n, onDismiss = { selected = null }, onChanged = bump) }
    if (confirmClear) ConfirmDialog(
        title = "Limpar histórico?",
        text = "Os registros de chamadas bloqueadas serão apagados. Suas listas não mudam.",
        confirm = "Limpar",
        onConfirm = { db.clearHistory(); confirmClear = false; bump() },
        onDismiss = { confirmClear = false }
    )
}
