package com.velarlabs.velarcall.ui

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.velarlabs.velarcall.data.Contact
import com.velarlabs.velarcall.data.ContactsRepo
import com.velarlabs.velarcall.data.ListType
import com.velarlabs.velarcall.data.PhoneNumbers
import com.velarlabs.velarcall.data.VelarDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

/** Agenda do telefone: cada contato pode ser bloqueado ou liberado. */
@Composable
fun ContactsScreen(tick: Int, bump: () -> Unit, env: AppEnv) {
    val context = LocalContext.current
    val db = remember { VelarDb.get(context) }
    val granted = remember(tick) { env.hasPermission(Manifest.permission.READ_CONTACTS) }

    if (!granted) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            EmptyState(
                Icons.Filled.Person, "Acesso à agenda",
                "Permita o acesso para ver seus contatos e bloquear ou liberar cada um.",
                Modifier.weight(1f, fill = false).padding(top = 80.dp)
            )
            Button(onClick = env.requestPermissions, shape = RoundedCornerShape(14.dp)) { Text("Permitir acesso") }
        }
        return
    }

    val contacts by produceState<List<Contact>?>(null, tick) {
        value = withContext(Dispatchers.IO) { ContactsRepo.load(context) }
    }
    val blockedKeys = remember(tick) { db.list(ListType.BLOCK).map { it.key }.toSet() }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Contact?>(null) }

    val all = contacts
    val shown = remember(all, query, filter, blockedKeys) {
        val q = norm(query)
        all.orEmpty().filter { c ->
            val isBlocked = c.keys.any { it in blockedKeys }
            (filter == 0 || (filter == 1 && isBlocked)) &&
                (q.isEmpty() || norm(c.name).contains(q) || c.keys.any { it.contains(PhoneNumbers.digits(query).ifEmpty { "\u0000" }) })
        }
    }
    val blockedTotal = remember(all, blockedKeys) { all.orEmpty().count { c -> c.keys.any { it in blockedKeys } } }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SearchField(query, { query = it }, "Buscar na agenda")
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("Todos (${all?.size ?: 0})", "Bloqueados ($blockedTotal)").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = filter == i, onClick = { filter = i },
                        shape = SegmentedButtonDefaults.itemShape(i, 2)
                    ) { Text(label) }
                }
            }
        }
        when {
            all == null -> EmptyState(null, "Carregando agenda…", "")
            shown.isEmpty() -> EmptyState(
                Icons.Filled.Person,
                if (filter == 1) "Nenhum contato bloqueado" else "Nada encontrado",
                if (filter == 1) "Toque em um contato para bloqueá-lo." else "Tente outro nome ou número."
            )
            else -> LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                items(shown, key = { it.id }) { c ->
                    val isBlocked = c.keys.any { it in blockedKeys }
                    ListRow(
                        title = c.name,
                        subtitle = PhoneNumbers.pretty(c.numbers.first()) +
                            if (c.numbers.size > 1) "  +${c.numbers.size - 1}" else "",
                        leading = { Avatar(c.name) },
                        trailing = {
                            if (isBlocked) Pill("Bloqueado", palette.danger, palette.dangerSoft)
                            else Pill("Liberado", palette.success, palette.successSoft)
                        },
                        onClick = { selected = c }
                    )
                }
            }
        }
    }

    selected?.let { c ->
        ContactSheet(c, db, onDismiss = { selected = null }, onChanged = bump)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactSheet(contact: Contact, db: VelarDb, onDismiss: () -> Unit, onChanged: () -> Unit) {
    var blocked by remember(contact.id) {
        mutableStateOf(contact.keys.any { k -> manualStatus(db, k) == ManualStatus.BLOCKED })
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(contact.name, size = 56.dp)
                Spacer(Modifier.size(14.dp))
                Column {
                    Text(contact.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (blocked) "Bloqueado: as ligações não tocam" else "Liberado: as ligações tocam normalmente",
                        color = if (blocked) palette.danger else palette.success,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Panel {
                contact.numbers.forEachIndexed { i, n ->
                    if (i > 0) RowDivider(16.dp)
                    ListRow(title = PhoneNumbers.pretty(n))
                }
            }
            Spacer(Modifier.height(16.dp))
            if (blocked) {
                Button(
                    onClick = {
                        contact.keys.forEach { setManual(db, it, ManualStatus.NONE) }
                        blocked = false; onChanged()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.success, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Liberar contato") }
            } else {
                Button(
                    onClick = {
                        contact.keys.forEach { setManual(db, it, ManualStatus.BLOCKED, contact.name) }
                        blocked = true; onChanged()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.danger, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Bloquear contato") }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Fechar") }
        }
    }
}

private fun norm(s: String): String =
    Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
