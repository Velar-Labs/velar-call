package com.velarlabs.velarcall.ui

import android.Manifest
import android.os.Build
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velarlabs.velarcall.data.CallDecider
import com.velarlabs.velarcall.data.Decision
import com.velarlabs.velarcall.data.ListType
import com.velarlabs.velarcall.data.PhoneNumbers
import com.velarlabs.velarcall.data.Settings
import com.velarlabs.velarcall.data.VelarDb
import com.velarlabs.velarcall.sync.BlocklistSync
import kotlinx.coroutines.launch
import java.util.Calendar

private data class Tab(val title: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Início", Icons.Filled.Home),
    Tab("Bloqueios", Icons.Filled.Call),
    Tab("Listas", Icons.AutoMirrored.Filled.List),
    Tab("Ajustes", Icons.Filled.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VelarApp(
    refreshTick: Int,
    initialTab: Int,
    isRoleHeld: () -> Boolean,
    requestRole: () -> Unit,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    // Incrementado localmente depois de cada alteração, para recarregar listas.
    var localTick by remember { mutableIntStateOf(0) }
    val tick = refreshTick + localTick
    val bump: () -> Unit = { localTick++ }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                                .background(VelarColors.TealDim),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Lock, null, tint = VelarColors.Teal, modifier = Modifier.size(17.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Text("VelarCall", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = VelarColors.Surface) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.title) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = VelarColors.TealDim,
                            selectedIconColor = VelarColors.Teal,
                            selectedTextColor = VelarColors.Teal,
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(tick, bump, isRoleHeld, requestRole, hasPermission, requestPermissions)
                1 -> HistoryScreen(tick, bump)
                2 -> ListsScreen(tick, bump)
                else -> SettingsScreen(tick, bump)
            }
        }
    }
}

// ------------------------------------------------------------------ Início

@Composable
private fun HomeScreen(
    tick: Int,
    bump: () -> Unit,
    isRoleHeld: () -> Boolean,
    requestRole: () -> Unit,
    hasPermission: (String) -> Boolean,
    requestPermissions: () -> Unit,
) {
    val context = LocalContext.current
    val db = remember { VelarDb.get(context) }
    val settings = remember { Settings(context) }
    val decider = remember { CallDecider(context) }

    val roleHeld = remember(tick) { isRoleHeld() }
    val contactsOk = remember(tick) { hasPermission(Manifest.permission.READ_CONTACTS) }
    val notifOk = remember(tick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }
    val protectionOn = remember(tick) { settings.protectionOn }
    val today = remember(tick) { db.blockedSince(startOfToday()) }
    val total = remember(tick) { db.blockedSince(0) }
    val online = remember(tick) { db.count(ListType.ONLINE) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val active = roleHeld && protectionOn
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (active) VelarColors.TealDim else VelarColors.DangerDim
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Icon(
                        if (active) Icons.Filled.CheckCircle else Icons.Filled.Warning, null,
                        tint = if (active) VelarColors.Teal else VelarColors.Danger,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (active) "Proteção ativa" else "Proteção desativada",
                        fontSize = 22.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            !roleHeld -> "Defina o VelarCall como app de identificação de chamadas e spam."
                            !protectionOn -> "A proteção está desligada nos ajustes."
                            else -> "Chamadas suspeitas e fora da agenda serão bloqueadas."
                        },
                        color = VelarColors.Muted
                    )
                    if (!roleHeld) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = requestRole) { Text("Ativar proteção") }
                    } else if (!protectionOn) {
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { settings.protectionOn = true; bump() }) { Text("Ligar") }
                    }
                }
            }
        }
        if (!contactsOk || !notifOk) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = VelarColors.Surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Permissões pendentes", fontWeight = FontWeight.SemiBold)
                        if (!contactsOk) Text(
                            "• Agenda: sem ela o app não sabe quem é contato e NÃO bloqueia desconhecidos.",
                            color = VelarColors.Muted
                        )
                        if (!notifOk) Text("• Notificações: para avisar quando bloquear.", color = VelarColors.Muted)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = requestPermissions) { Text("Conceder") }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Stat("Hoje", today.toString(), Modifier.weight(1f))
                Stat("Total", total.toString(), Modifier.weight(1f))
                Stat("Lista online", online.toString(), Modifier.weight(1f))
            }
        }
        item { LookupCard(decider, db, bump) }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = VelarColors.Surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = VelarColors.Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LookupCard(decider: CallDecider, db: VelarDb, bump: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<Decision?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = VelarColors.Surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Consultar número", fontWeight = FontWeight.SemiBold)
            Text("Veja o que o VelarCall faria se esse número ligasse.", color = VelarColors.Muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; result = null },
                    placeholder = { Text("(11) 91234-5678") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    result = if (PhoneNumbers.key(input).isEmpty()) null
                    else decider.decide(input, ignoreMasterSwitch = true)
                }) { Icon(Icons.Filled.Search, "Consultar") }
            }
            result?.let { r ->
                Spacer(Modifier.height(10.dp))
                val blocked = r is Decision.Block
                val reason = when (r) { is Decision.Block -> r.reason; is Decision.Allow -> r.reason }
                Text(
                    if (blocked) "Seria bloqueado" else "Seria permitido",
                    color = if (blocked) VelarColors.Danger else VelarColors.Teal,
                    fontWeight = FontWeight.Bold
                )
                Text(reason, color = VelarColors.Muted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val key = PhoneNumbers.key(input)
                    OutlinedButton(onClick = {
                        db.put(key, ListType.ALLOW, ""); result = decider.decide(input, ignoreMasterSwitch = true); bump()
                    }) { Text("Permitir") }
                    Button(
                        onClick = {
                            db.put(key, ListType.BLOCK, ""); result = decider.decide(input, ignoreMasterSwitch = true); bump()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VelarColors.Danger)
                    ) { Text("Bloquear") }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Bloqueios

@Composable
private fun HistoryScreen(tick: Int, bump: () -> Unit) {
    val context = LocalContext.current
    val db = remember { VelarDb.get(context) }
    val calls = remember(tick) { db.blocked() }
    var selected by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    if (calls.isEmpty()) {
        Empty("Nenhuma chamada bloqueada ainda.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${calls.size} registros", color = VelarColors.Muted)
                TextButton(onClick = { confirmClear = true }) { Text("Limpar") }
            }
        }
        items(calls, key = { it.id }) { c ->
            Row(
                Modifier.fillMaxWidth().clickable { selected = c.number }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(VelarColors.DangerDim),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Call, null, tint = VelarColors.Danger, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(PhoneNumbers.pretty(c.number), fontWeight = FontWeight.SemiBold)
                    Text(c.reason, color = VelarColors.Muted, fontSize = 13.sp)
                }
                Text(
                    DateUtils.getRelativeTimeSpanString(c.at).toString(),
                    color = VelarColors.Muted, fontSize = 12.sp
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        }
    }

    selected?.let { num ->
        val key = PhoneNumbers.key(num)
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(PhoneNumbers.pretty(num)) },
            text = {
                Text(
                    if (key.isEmpty()) "Número oculto não pode ser adicionado às listas."
                    else "Adicionar à lista permitida faz este número tocar normalmente."
                )
            },
            confirmButton = {
                if (key.isNotEmpty()) TextButton(onClick = {
                    db.put(key, ListType.ALLOW, ""); selected = null; bump()
                }) { Text("Permitir sempre") }
            },
            dismissButton = {
                if (key.isNotEmpty()) TextButton(onClick = {
                    db.put(key, ListType.BLOCK, ""); selected = null; bump()
                }) { Text("Bloquear sempre") }
                else TextButton(onClick = { selected = null }) { Text("Fechar") }
            }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Limpar histórico?") },
            text = { Text("Os registros de chamadas bloqueadas serão apagados. As listas não mudam.") },
            confirmButton = {
                TextButton(onClick = { db.clearHistory(); confirmClear = false; bump() }) { Text("Limpar") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } }
        )
    }
}

// ------------------------------------------------------------------ Listas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListsScreen(tick: Int, bump: () -> Unit) {
    val context = LocalContext.current
    val db = remember { VelarDb.get(context) }
    var which by rememberSaveable { mutableIntStateOf(0) }
    val type = if (which == 0) ListType.ALLOW else ListType.BLOCK
    val entries = remember(tick, which) { db.list(type) }
    var adding by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                listOf("Permitidos", "Bloqueados").forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = which == i,
                        onClick = { which = i },
                        shape = SegmentedButtonDefaults.itemShape(i, 2)
                    ) { Text(label) }
                }
            }
            Text(
                if (which == 0) "Sempre tocam, mesmo fora da agenda."
                else "Sempre bloqueados, mesmo com outras regras desligadas.",
                color = VelarColors.Muted, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (entries.isEmpty()) {
                Empty("Lista vazia. Toque em + para adicionar.")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)) {
                    items(entries, key = { it.key }) { e ->
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(PhoneNumbers.pretty(e.key), fontWeight = FontWeight.SemiBold)
                                if (e.label.isNotBlank()) Text(e.label, color = VelarColors.Muted, fontSize = 13.sp)
                            }
                            IconButton(onClick = { db.remove(e.key, type); bump() }) {
                                Icon(Icons.Filled.Delete, "Remover", tint = VelarColors.Muted)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { adding = true },
            containerColor = VelarColors.Teal,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Filled.Add, "Adicionar") }
    }

    if (adding) {
        AddNumberDialog(
            title = if (which == 0) "Permitir número" else "Bloquear número",
            onDismiss = { adding = false },
            onSave = { num, label -> db.put(PhoneNumbers.key(num), type, label); adding = false; bump() }
        )
    }
}

@Composable
private fun AddNumberDialog(title: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var num by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val valid = PhoneNumbers.key(num).length >= 8
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = num, onValueChange = { num = it }, singleLine = true,
                    label = { Text("Número com DDD") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(
                    value = label, onValueChange = { label = it.take(40) }, singleLine = true,
                    label = { Text("Nome ou motivo (opcional)") }
                )
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(num, label.trim()) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ------------------------------------------------------------------ Ajustes

@Composable
private fun SettingsScreen(tick: Int, bump: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val db = remember { VelarDb.get(context) }
    val scope = rememberCoroutineScope()
    var syncing by remember { mutableStateOf(false) }
    var url by rememberSaveable { mutableStateOf(settings.onlineListUrl) }
    val syncMsg = remember(tick) { settings.lastSyncMessage }
    val syncAt = remember(tick) { settings.lastSyncAt }
    val onlineCount = remember(tick) { db.count(ListType.ONLINE) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { Section("Proteção") }
        item { Toggle("Proteção ligada", "Chave geral do bloqueio.", settings.protectionOn) { settings.protectionOn = it; bump() } }
        item {
            Toggle(
                "Bloquear quem não está na agenda",
                "Só tocam contatos e números da lista permitida.",
                settings.blockUnknown
            ) { settings.blockUnknown = it; bump() }
        }
        item { Toggle("Bloquear números ocultos", "Chamadas sem identificação.", settings.blockHidden) { settings.blockHidden = it; bump() } }
        item {
            Toggle(
                "Bloquear prefixo 0303",
                "Prefixo de telemarketing. Desde 2025 não é mais obrigatório, então pega só parte do telemarketing.",
                settings.blockTelemarketingPrefix
            ) { settings.blockTelemarketingPrefix = it; bump() }
        }
        item {
            Toggle(
                "Bloquear origem falsificada",
                "Quando a operadora indica que o número foi forjado (Android 11+).",
                settings.blockSpoofed
            ) { settings.blockSpoofed = it; bump() }
        }
        item { Toggle("Avisar quando bloquear", "Notificação discreta a cada bloqueio.", settings.notifyOnBlock) { settings.notifyOnBlock = it; bump() } }

        item { Spacer(Modifier.height(12.dp)); Section("Lista online") }
        item {
            Toggle(
                "Usar lista da internet",
                "Baixa números suspeitos a cada 12 h. $onlineCount números carregados.",
                settings.useOnlineList
            ) { settings.useOnlineList = it; bump() }
        }
        item {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Endereço da lista (https)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Button(
                    enabled = !syncing,
                    onClick = {
                        settings.onlineListUrl = url
                        syncing = true
                        scope.launch {
                            BlocklistSync.run(context)
                            syncing = false
                            bump()
                        }
                    }
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (syncing) "Sincronizando…" else "Sincronizar agora")
                }
                Spacer(Modifier.width(10.dp))
                TextButton(onClick = { url = Settings.DEFAULT_LIST_URL }) { Text("Padrão") }
            }
        }
        item {
            Text(
                buildString {
                    append(syncMsg)
                    if (syncAt > 0) append(" · ").append(DateUtils.getRelativeTimeSpanString(syncAt))
                },
                color = VelarColors.Muted, fontSize = 13.sp
            )
        }
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "VelarCall 0.1 · Velar Labs. O bloqueio acontece no aparelho; nenhum dado das suas chamadas sai do celular.",
                color = VelarColors.Muted, fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(), color = VelarColors.Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun Toggle(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChange(!value) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title)
            Text(subtitle, color = VelarColors.Muted, fontSize = 13.sp)
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun Empty(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = VelarColors.Muted)
    }
}

private fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

