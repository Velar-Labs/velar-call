package com.velarlabs.velarcall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.velarlabs.velarcall.data.ListType
import com.velarlabs.velarcall.data.PhoneNumbers
import com.velarlabs.velarcall.data.VelarDb
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(tick: Int, bump: () -> Unit, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val db = remember { VelarDb.get(context) }
    val scope = rememberCoroutineScope()
    var which by rememberSaveable { mutableIntStateOf(1) }
    val type = if (which == 0) ListType.ALLOW else ListType.BLOCK
    val entries = remember(tick, which) { db.list(type) }
    var query by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }

    val shown = remember(entries, query) {
        val d = PhoneNumbers.digits(query)
        entries.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) || (d.isNotEmpty() && it.key.contains(d)) }
    }

    Box(Modifier.fillMaxSize()) {
        Column {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Liberados", "Bloqueados").forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = which == i, onClick = { which = i },
                            shape = SegmentedButtonDefaults.itemShape(i, 2)
                        ) { Text(label) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                SearchField(query, { query = it }, "Buscar número ou nome")
            }
            if (shown.isEmpty()) {
                EmptyState(
                    Icons.AutoMirrored.Filled.List,
                    if (query.isNotBlank()) "Nada encontrado" else if (which == 0) "Nenhum número liberado" else "Nenhum número bloqueado",
                    if (which == 0) "Números liberados tocam mesmo fora da agenda."
                    else "Números bloqueados nunca tocam, nem se forem contatos."
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)) {
                    item {
                        Panel {
                            shown.forEachIndexed { i, e ->
                                if (i > 0) RowDivider()
                                ListRow(
                                    title = e.label.ifBlank { PhoneNumbers.pretty(e.key) },
                                    subtitle = if (e.label.isBlank()) null else PhoneNumbers.pretty(e.key),
                                    leading = { Avatar(e.label.ifBlank { e.key }) },
                                    trailing = {
                                        IconButton(onClick = {
                                            db.remove(e.key, type); bump()
                                            scope.launch {
                                                val r = snackbar.showSnackbar("Número removido", "Desfazer", withDismissAction = true)
                                                if (r == SnackbarResult.ActionPerformed) { db.put(e.key, type, e.label); bump() }
                                            }
                                        }) { Icon(Icons.Filled.Delete, "Remover", tint = palette.muted) }
                                    },
                                    onClick = { selected = e.key }
                                )
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text(if (which == 0) "Liberar número" else "Bloquear número") },
            containerColor = palette.brand,
            contentColor = palette.onBrand,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    if (adding) AddNumberDialog(
        title = if (which == 0) "Liberar número" else "Bloquear número",
        onDismiss = { adding = false },
        onSave = { n, label ->
            setManual(db, PhoneNumbers.key(n), if (which == 0) ManualStatus.ALLOWED else ManualStatus.BLOCKED, label)
            adding = false; bump()
        }
    )
    selected?.let { k -> NumberSheet(raw = k, onDismiss = { selected = null }, onChanged = bump) }
}

