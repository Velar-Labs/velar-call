package com.velarlabs.velarcall.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.velarlabs.velarcall.data.ListType
import com.velarlabs.velarcall.data.PhoneNumbers
import com.velarlabs.velarcall.data.Settings
import com.velarlabs.velarcall.data.VelarDb
import com.velarlabs.velarcall.sync.BlocklistSync
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    tick: Int,
    bump: () -> Unit,
    env: AppEnv,
    goTo: (Int) -> Unit,
) {
    val context = LocalContext.current
    val db = remember { VelarDb.get(context) }
    val settings = remember { Settings(context) }
    val scope = rememberCoroutineScope()

    val roleHeld = remember(tick) { env.isRoleHeld() }
    val contactsOk = remember(tick) { env.hasPermission(Manifest.permission.READ_CONTACTS) }
    val notifOk = remember(tick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || env.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }
    val protectionOn = remember(tick) { settings.protectionOn }
    val today = remember(tick) { db.blockedSince(startOfToday()) }
    val total = remember(tick) { db.blockedSince(0) }
    val recent = remember(tick) { db.blocked(4) }
    val onlineCount = remember(tick) { db.count(ListType.ONLINE) }
    val blockedCount = remember(tick) { db.count(ListType.BLOCK) }
    val weekStart = remember(tick) { startOfToday() - 6 * android.text.format.DateUtils.DAY_IN_MILLIS }
    val weekCount = remember(tick) { db.blockedSince(weekStart) }
    val perDay = remember(tick) { db.blockedPerDay(7, startOfToday(), android.text.format.DateUtils.DAY_IN_MILLIS) }
    val topReasons = remember(tick) { db.topReasonsSince(weekStart, 3) }

    var lookup by remember { mutableStateOf(false) }
    var sheetNumber by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }

    val active = roleHeld && protectionOn

    LazyColumn(contentPadding = ScreenPadding) {
        // ---------- status
        item {
            val bg = if (active) HeroBlue else palette.dangerSoft
            val fg = if (active) Color.White else palette.text
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(bg).padding(24.dp)
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape)
                        .background(if (active) Color.White.copy(alpha = 0.18f) else palette.danger.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        shieldPainter(), null, modifier = Modifier.size(30.dp),
                        tint = if (active) Color.White else palette.danger
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    if (active) "Você está protegido" else "Proteção desativada",
                    style = MaterialTheme.typography.headlineMedium, color = fg
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        !roleHeld -> "Ative o VelarCall como app de identificação de chamadas para começar a bloquear."
                        !protectionOn -> "A proteção foi desligada nos ajustes."
                        today == 0 -> "Nenhuma chamada bloqueada hoje."
                        today == 1 -> "1 chamada bloqueada hoje."
                        else -> "$today chamadas bloqueadas hoje."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (active) Color.White.copy(alpha = 0.85f) else palette.muted
                )
                if (!active) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { if (!roleHeld) env.requestRole() else { settings.protectionOn = true; bump() } },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.danger, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Ativar proteção") }
                } else {
                    Spacer(Modifier.height(20.dp))
                    Row {
                        HeroStat("$total", "no total", Modifier.weight(1f))
                        HeroStat("$blockedCount", "sua lista", Modifier.weight(1f))
                        HeroStat("$onlineCount", "lista online", Modifier.weight(1f))
                    }
                }
            }
        }

        // ---------- permissões
        if (!contactsOk || !notifOk) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(palette.warningSoft)
                        .clickable(onClick = env.requestPermissions).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, null, tint = palette.warning)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (!contactsOk) "Permita o acesso à agenda" else "Permita as notificações",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (!contactsOk) "Sem a agenda, o app não sabe quem é contato e não bloqueia desconhecidos."
                            else "Para avisar quando uma chamada for bloqueada.",
                            style = MaterialTheme.typography.bodySmall, color = palette.muted
                        )
                    }
                    Text("Permitir", color = palette.warning, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // ---------- ações rápidas
        item {
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuickAction(Icons.Filled.Search, "Consultar") { lookup = true }
                QuickAction(Icons.Filled.Add, "Bloquear\nnúmero") { adding = true }
                QuickAction(Icons.Filled.Person, "Agenda") { goTo(TAB_CONTACTS) }
                QuickAction(Icons.Filled.Refresh, if (syncing) "Atualizando" else "Atualizar\nlista") {
                    if (!syncing) {
                        syncing = true
                        scope.launch { BlocklistSync.run(context); syncing = false; bump() }
                    }
                }
            }
        }

        // ---------- resumo da semana
        if (total > 0) {
            item {
                SectionLabel("ÚLTIMOS 7 DIAS")
                Panel {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            "$weekCount ${if (weekCount == 1) "chamada bloqueada" else "chamadas bloqueadas"}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        WeekBars(perDay)
                        if (topReasons.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            topReasons.forEachIndexed { i, (reason, count) ->
                                if (i > 0) Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(reason, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text("$count", style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---------- recentes
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("BLOQUEADAS RECENTEMENTE", Modifier.weight(1f))
                if (recent.isNotEmpty()) TextButton(onClick = { goTo(TAB_HISTORY) }) { Text("Ver tudo") }
            }
            Panel {
                if (recent.isEmpty()) {
                    Text(
                        "Quando o VelarCall bloquear uma chamada, ela aparece aqui.",
                        color = palette.muted, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                recent.forEachIndexed { i, c ->
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
                            Text(
                                if (c.at >= startOfToday()) timeLabel(c.at) else dayLabel(c.at),
                                style = MaterialTheme.typography.bodySmall, color = palette.muted
                            )
                        },
                        onClick = { sheetNumber = c.number }
                    )
                }
            }
        }
    }

    if (lookup) LookupSheet(onDismiss = { lookup = false }, onOpenNumber = { lookup = false; sheetNumber = it })
    sheetNumber?.let { n -> NumberSheet(raw = n, onDismiss = { sheetNumber = null }, onChanged = bump) }
    if (adding) AddNumberDialog(
        title = "Bloquear número",
        onDismiss = { adding = false },
        onSave = { n, label -> setManual(db, PhoneNumbers.key(n), ManualStatus.BLOCKED, label); adding = false; bump() }
    )
}

/** Azul da marca fixo no card de status: mantém contraste com texto branco nos dois temas. */
private val HeroBlue = Color(0xFF2F5BFF)

@Composable
private fun WeekBars(perDay: IntArray) {
    val max = (perDay.maxOrNull() ?: 0).coerceAtLeast(1)
    val letters = remember(perDay) {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = startOfToday()
        val l = CharArray(7)
        for (i in 6 downTo 0) {
            l[i] = "DSTQQSS"[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
        l
    }
    Column {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.Bottom) {
            perDay.forEachIndexed { i, v ->
                val today = i == perDay.lastIndex
                Box(
                    Modifier.weight(1f).padding(horizontal = 3.dp)
                        .height((6 + (v.toFloat() / max) * 48).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (today) palette.brand else palette.brandSoft)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            letters.forEach { ch ->
                Text(
                    ch.toString(), style = MaterialTheme.typography.labelMedium, color = palette.muted,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.width(80.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(palette.surface),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = palette.brand) }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, color = palette.text)
    }
}
