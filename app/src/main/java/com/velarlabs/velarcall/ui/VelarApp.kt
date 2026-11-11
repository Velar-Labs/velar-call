package com.velarlabs.velarcall.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.velarlabs.velarcall.data.Settings

/** Ações que dependem da Activity (papel do sistema e permissões). */
class AppEnv(
    val isRoleHeld: () -> Boolean,
    val requestRole: () -> Unit,
    val hasPermission: (String) -> Boolean,
    val requestPermissions: () -> Unit,
)

const val TAB_HOME = 0
const val TAB_HISTORY = 1
const val TAB_CONTACTS = 2
const val TAB_LISTS = 3
const val TAB_SETTINGS = 4

private val titles = listOf("VelarCall", "Recados", "Agenda", "Minhas listas", "Ajustes")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VelarApp(refreshTick: Int, initialTab: Int, env: AppEnv) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var localTick by remember { mutableIntStateOf(0) }
    val tick = refreshTick + localTick
    val bump: () -> Unit = { localTick++ }
    var onboarded by remember { mutableStateOf(settings.onboardingDone) }
    val snackbar = remember { SnackbarHostState() }

    if (!onboarded) {
        Onboarding(tick, env) { settings.onboardingDone = true; onboarded = true }
        return
    }

    val navItems: List<Pair<String, Painter>> = listOf(
        "Início" to shieldPainter(),
        "Recados" to voicemailPainter(),
        "Agenda" to rememberVectorPainter(Icons.Filled.Person),
        "Listas" to rememberVectorPainter(Icons.AutoMirrored.Filled.List),
        "Ajustes" to rememberVectorPainter(Icons.Filled.Settings),
    )

    Scaffold(
        containerColor = palette.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (tab == TAB_HOME) {
                            Box(
                                Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(palette.brand),
                                contentAlignment = Alignment.Center
                            ) { Icon(shieldPainter(), null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(titles[tab], style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.bg, titleContentColor = palette.text)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = palette.surface, tonalElevation = 0.dp) {
                navItems.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, null, modifier = Modifier.size(24.dp)) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = palette.brandSoft,
                            selectedIconColor = palette.brand,
                            selectedTextColor = palette.brand,
                            unselectedIconColor = palette.muted,
                            unselectedTextColor = palette.muted,
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                TAB_HOME -> HomeScreen(tick, bump, env) { tab = it }
                TAB_HISTORY -> RecadosScreen(tick, bump)
                TAB_CONTACTS -> ContactsScreen(tick, bump, env)
                TAB_LISTS -> ListsScreen(tick, bump, snackbar)
                else -> SettingsScreen(tick, bump, env)
            }
        }
    }
}

// ------------------------------------------------------------------ primeira abertura

@Composable
private fun Onboarding(tick: Int, env: AppEnv, onDone: () -> Unit) {
    val roleHeld = remember(tick) { env.isRoleHeld() }
    val contactsOk = remember(tick) { env.hasPermission(Manifest.permission.READ_CONTACTS) }
    val notifOk = remember(tick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || env.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(
        Modifier.fillMaxSize().background(palette.bg).safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Box(
            Modifier.size(88.dp).clip(RoundedCornerShape(26.dp)).background(palette.brand),
            contentAlignment = Alignment.Center
        ) { Icon(shieldPainter(), null, tint = Color.White, modifier = Modifier.size(52.dp)) }
        Spacer(Modifier.height(20.dp))
        Text("Bem-vindo ao VelarCall", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            "Bloqueie golpes, telemarketing e quem não está na sua agenda. Três passos e está pronto.",
            style = MaterialTheme.typography.bodyMedium, color = palette.muted, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Step(
                1, "Ativar proteção", "Defina o VelarCall como app de identificação de chamadas.",
                done = roleHeld, onClick = env.requestRole
            )
            Step(
                2, "Acesso à agenda", "Para seus contatos sempre tocarem.",
                done = contactsOk, onClick = env.requestPermissions
            )
            Step(
                3, "Notificações", "Para avisar quando bloquear uma chamada.",
                done = notifOk, onClick = env.requestPermissions, icon = Icons.Filled.Notifications
            )
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onDone, shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (roleHeld && contactsOk) "Começar" else "Continuar assim mesmo") }
        if (!(roleHeld && contactsOk)) TextButton(onClick = onDone) { Text("Configurar depois", color = palette.muted) }
    }
}

@Composable
private fun Step(
    n: Int, title: String, text: String, done: Boolean, onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Panel {
        ListRow(
            title = title,
            subtitle = text,
            subtitleLines = 2,
            leading = {
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(if (done) palette.successSoft else palette.brandSoft),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        done -> Icon(Icons.Filled.Check, null, tint = palette.success)
                        icon != null -> Icon(icon, null, tint = palette.brand, modifier = Modifier.size(20.dp))
                        else -> Text("$n", color = palette.brand, style = MaterialTheme.typography.titleMedium)
                    }
                }
            },
            trailing = {
                if (done) Pill("Pronto", palette.success, palette.successSoft)
                else Pill("Ativar", palette.onBrand, palette.brand, onClick)
            },
            onClick = if (done) null else onClick
        )
    }
}
