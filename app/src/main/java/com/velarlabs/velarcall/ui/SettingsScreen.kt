package com.velarlabs.velarcall.ui

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.unit.dp
import com.velarlabs.velarcall.data.Backup
import com.velarlabs.velarcall.data.ListType
import com.velarlabs.velarcall.data.PhoneNumbers
import com.velarlabs.velarcall.data.QuietHours
import com.velarlabs.velarcall.data.Settings
import com.velarlabs.velarcall.data.VelarDb
import com.velarlabs.velarcall.sync.BlocklistSync
import android.text.format.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(tick: Int, bump: () -> Unit, env: AppEnv) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val db = remember { VelarDb.get(context) }
    val scope = rememberCoroutineScope()
    var syncing by remember { mutableStateOf(false) }
    var url by rememberSaveable { mutableStateOf(settings.onlineListUrl) }
    val syncMsg = remember(tick) { settings.lastSyncMessage }
    val syncAt = remember(tick) { settings.lastSyncAt }
    val onlineCount = remember(tick) { db.count(ListType.ONLINE) }
    val roleHeld = remember(tick) { env.isRoleHeld() }
    val diag = remember(tick) { settings.lastCallDiag }
    val diagAt = remember(tick) { settings.lastCallDiagAt }
    var prefixInput by rememberSaveable { mutableStateOf("") }
    var backupMsg by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(Backup.export(db, settings).toByteArray()) }
            }.isSuccess
            backupMsg = if (ok) "Backup salvo com sucesso." else "Não consegui salvar o backup."
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val r = runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                Backup.import(db, settings, text)
            }
            backupMsg = r.fold(
                { "Importado: ${it.allow} liberados, ${it.block} bloqueados, ${it.prefixes} prefixos." },
                { "Não consegui ler esse arquivo." }
            )
            bump()
        }
    }

    val pickTime: (Int, (Int) -> Unit) -> Unit = { initial, onSet ->
        TimePickerDialog(context, { _, h, m -> onSet(h * 60 + m) }, initial / 60, initial % 60, true).show()
    }

    LazyColumn(contentPadding = ScreenPadding) {
        item { SectionLabel("PROTEÇÃO") }
        item {
            Panel {
                ToggleRow("Proteção ligada", "Chave geral de todos os bloqueios.", settings.protectionOn) {
                    settings.protectionOn = it; bump()
                }
                RowDivider(16.dp)
                ListRow(
                    title = "App de identificação de chamadas",
                    subtitle = if (roleHeld) "VelarCall está ativo" else "Toque para ativar",
                    subtitleColor = if (roleHeld) palette.success else palette.danger,
                    onClick = if (roleHeld) null else env.requestRole
                )
            }
        }

        if (diag.isNotBlank()) {
            item { SectionLabel("ÚLTIMA CHAMADA ANALISADA") }
            item {
                Panel {
                    Column(Modifier.padding(16.dp)) {
                        Text(diag, style = MaterialTheme.typography.bodyMedium, color = palette.text)
                        if (diagAt > 0) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                DateUtils.getRelativeTimeSpanString(diagAt).toString(),
                                style = MaterialTheme.typography.bodySmall, color = palette.muted
                            )
                        }
                    }
                }
            }
        }

        item { SectionLabel("O QUE BLOQUEAR") }
        item {
            Panel {
                ToggleRow(
                    "Quem não está na agenda",
                    "Só tocam contatos e números liberados.",
                    settings.blockUnknown
                ) { settings.blockUnknown = it; bump() }
                RowDivider(16.dp)
                ToggleRow(
                    "Deixar tocar se insistir",
                    "Se o mesmo número desconhecido ligar de novo em 3 minutos, a segunda chamada toca. Pode ser urgente.",
                    settings.allowRepeatCaller
                ) { settings.allowRepeatCaller = it; bump() }
                RowDivider(16.dp)
                ToggleRow(
                    "Números ocultos",
                    "Chamadas sem identificador. Deixe desligado se às vezes não recebe chamadas de contatos: alguns aparelhos não entregam o número ao app.",
                    settings.blockHidden
                ) {
                    settings.blockHidden = it; bump()
                }
                RowDivider(16.dp)
                ToggleRow(
                    "Telemarketing 0303",
                    "Desde 2025 o prefixo não é obrigatório, então pega só parte do telemarketing.",
                    settings.blockTelemarketingPrefix
                ) { settings.blockTelemarketingPrefix = it; bump() }
                RowDivider(16.dp)
                ToggleRow(
                    "Número forjado",
                    "Quando a operadora indica origem falsa (Android 11+). Muitas operadoras no Brasil não informam isso de forma confiável, por isso vem desligado.",
                    settings.blockSpoofed
                ) { settings.blockSpoofed = it; bump() }
            }
        }

        item { SectionLabel("LISTA ONLINE") }
        item {
            Panel {
                ToggleRow(
                    "Usar lista da internet",
                    "$onlineCount números suspeitos carregados. Atualiza a cada 12 horas.",
                    settings.useOnlineList
                ) { settings.useOnlineList = it; bump() }
                RowDivider(16.dp)
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = url, onValueChange = { url = it },
                        label = { Text("Endereço da lista (https)") },
                        singleLine = true, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !syncing, shape = RoundedCornerShape(14.dp),
                            onClick = {
                                settings.onlineListUrl = url
                                syncing = true
                                scope.launch { BlocklistSync.run(context); syncing = false; bump() }
                            }
                        ) { Text(if (syncing) "Atualizando…" else "Atualizar agora") }
                        TextButton(onClick = { url = Settings.DEFAULT_LIST_URL }) { Text("Usar padrão") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        buildString {
                            append(syncMsg)
                            if (syncAt > 0) append(" · ").append(DateUtils.getRelativeTimeSpanString(syncAt))
                        },
                        style = MaterialTheme.typography.bodySmall, color = palette.muted
                    )
                }
            }
        }

        // ---------- bloquear por prefixo
        item { SectionLabel("BLOQUEAR POR PADRÃO") }
        item {
            val myPrefixes = remember(tick) { settings.blockedPrefixes }
            Panel {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Bloqueia todo número que começa com um prefixo. Toque para ligar/desligar os comuns ou adicione um (pode ser um DDD, como 11). Contatos não são afetados.",
                        style = MaterialTheme.typography.bodySmall, color = palette.muted
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("0800", "0300", "0500", "0900").forEach { p ->
                            val on = p in myPrefixes
                            Pill(
                                p,
                                fg = if (on) palette.onBrand else palette.brand,
                                bg = if (on) palette.brand else palette.brandSoft
                            ) {
                                settings.blockedPrefixes = if (on) myPrefixes - p else myPrefixes + p; bump()
                            }
                        }
                    }
                    val custom = myPrefixes.filterNot { it in setOf("0800", "0300", "0500", "0900") }
                    if (custom.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        custom.forEach { p ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                IconButton(onClick = { settings.blockedPrefixes = myPrefixes - p; bump() }) {
                                    Icon(Icons.Filled.Close, "Remover", tint = palette.muted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = prefixInput,
                            onValueChange = { prefixInput = it.filter(Char::isDigit).take(6) },
                            label = { Text("Novo prefixo") },
                            singleLine = true, shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            enabled = prefixInput.length >= 2,
                            shape = RoundedCornerShape(14.dp),
                            onClick = { settings.blockedPrefixes = myPrefixes + prefixInput; prefixInput = ""; bump() }
                        ) { Text("Adicionar") }
                    }
                }
            }
        }

        // ---------- horário silencioso
        item { SectionLabel("HORÁRIO SILENCIOSO") }
        item {
            Panel {
                ToggleRow(
                    "Horário silencioso",
                    "Nesse intervalo, só contatos e números liberados tocam. O resto vai direto para o tratamento de bloqueio.",
                    settings.quietHoursEnabled
                ) { settings.quietHoursEnabled = it; bump() }
                if (settings.quietHoursEnabled) {
                    RowDivider(16.dp)
                    ListRow(
                        title = "Início",
                        subtitle = QuietHours.format(settings.quietStart),
                        onClick = { pickTime(settings.quietStart) { settings.quietStart = it; bump() } }
                    )
                    RowDivider(16.dp)
                    ListRow(
                        title = "Fim",
                        subtitle = QuietHours.format(settings.quietEnd),
                        onClick = { pickTime(settings.quietEnd) { settings.quietEnd = it; bump() } }
                    )
                    RowDivider(16.dp)
                    Column(Modifier.padding(16.dp)) {
                        Text("Dias", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        val days = remember(tick) { settings.quietDays }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            "DSTQQSS".forEachIndexed { i, ch ->
                                val day = i + 1
                                val eff = if (days.isEmpty()) (1..7).toSet() else days
                                DayChip(ch.toString(), day in eff) {
                                    val ne = if (day in eff) eff - day else eff + day
                                    settings.quietDays = when {
                                        ne.size == 7 -> emptySet()
                                        ne.isEmpty() -> eff
                                        else -> ne
                                    }
                                    bump()
                                }
                            }
                        }
                    }
                }
            }
        }

        item { SectionLabel("NOTIFICAÇÕES") }
        item {
            Panel {
                ToggleRow("Avisar quando bloquear", "Uma notificação discreta a cada chamada bloqueada.", settings.notifyOnBlock) {
                    settings.notifyOnBlock = it; bump()
                }
            }
        }

        // ---------- backup
        item { SectionLabel("BACKUP E RESTAURAÇÃO") }
        item {
            Panel {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Salve suas listas e prefixos num arquivo e restaure depois ou em outro celular.",
                        style = MaterialTheme.typography.bodyMedium, color = palette.muted
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(shape = RoundedCornerShape(14.dp), onClick = { exportLauncher.launch("velarcall-backup.json") }) {
                            Text("Exportar")
                        }
                        OutlinedButton(shape = RoundedCornerShape(14.dp), onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                            Text("Importar")
                        }
                    }
                    if (backupMsg.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(backupMsg, style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(28.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(shieldPainter(), null, tint = palette.brand, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(6.dp))
                Text("VelarCall · Velar Labs", style = MaterialTheme.typography.labelLarge)
                Text(
                    "As chamadas são analisadas no seu celular. Nenhum dado delas é enviado.",
                    style = MaterialTheme.typography.bodySmall, color = palette.muted
                )
            }
        }
    }
}

@Composable
private fun DayChip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(if (on) palette.brand else palette.surfaceAlt)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (on) palette.onBrand else palette.muted,
            textAlign = TextAlign.Center
        )
    }
}
