package com.velarlabs.velarcall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.velarlabs.velarcall.data.ListType
import com.velarlabs.velarcall.data.Settings
import com.velarlabs.velarcall.data.VelarDb
import com.velarlabs.velarcall.sync.BlocklistSync
import android.text.format.DateUtils
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

        item { SectionLabel("NOTIFICAÇÕES") }
        item {
            Panel {
                ToggleRow("Avisar quando bloquear", "Uma notificação discreta a cada chamada bloqueada.", settings.notifyOnBlock) {
                    settings.notifyOnBlock = it; bump()
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
