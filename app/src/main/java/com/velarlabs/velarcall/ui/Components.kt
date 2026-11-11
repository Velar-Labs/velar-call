package com.velarlabs.velarcall.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velarlabs.velarcall.R
import java.util.Calendar

@Composable
fun shieldPainter(): Painter = painterResource(R.drawable.ic_shield_v)

private val avatarColors = listOf(
    Color(0xFF2F5BFF), Color(0xFF0EA5E9), Color(0xFF10B981), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF14B8A6),
)

/** Avatar com iniciais, como na agenda do Android. */
@Composable
fun Avatar(name: String, size: Dp = 44.dp, tint: Color? = null, content: (@Composable () -> Unit)? = null) {
    val base = tint ?: avatarColors[(name.hashCode() and Int.MAX_VALUE) % avatarColors.size]
    Box(
        Modifier.size(size).clip(CircleShape).background(base.copy(alpha = if (palette.dark) 0.28f else 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) content()
        else Text(
            initials(name), color = base, fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.38f).sp
        )
    }
}

fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() && it.first().isLetter() }
    return when {
        parts.isEmpty() -> "#"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

/** Cartão branco com cantos arredondados (agrupa linhas, estilo ajustes do Android). */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(palette.surface)
            .border(1.dp, palette.outline, RoundedCornerShape(20.dp)),
        content = content
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.labelMedium, color = palette.muted,
        modifier = modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun RowDivider(start: Dp = 72.dp) {
    HorizontalDivider(Modifier.padding(start = start), color = palette.outline)
}

/** Linha de lista: ícone/avatar, título, subtítulo e um elemento à direita. */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    subtitleColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading(); Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(
                subtitle, style = MaterialTheme.typography.bodySmall,
                color = subtitleColor ?: palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp)); trailing()
        }
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.muted)
        }
        Switch(
            checked = value, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = palette.brand, checkedThumbColor = palette.onBrand)
        )
    }
}

/** Etiqueta pequena colorida ("Bloqueado", "Permitido"). */
@Composable
fun Pill(text: String, fg: Color, bg: Color, onClick: (() -> Unit)? = null) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) { Text(text, color = fg, style = MaterialTheme.typography.labelMedium) }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(palette.surfaceAlt)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = palette.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = palette.muted, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.text),
                cursorBrush = SolidColor(palette.brand),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) {
            Icon(Icons.Filled.Close, "Limpar", tint = palette.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector?, title: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(palette.brandSoft),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) Icon(icon, null, tint = palette.brand, modifier = Modifier.size(32.dp))
            else Icon(shieldPainter(), null, tint = palette.brand, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
    }
}

val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)

fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun dayLabel(at: Long): String {
    val today = startOfToday()
    return when {
        at >= today -> "Hoje"
        at >= today - DateUtils.DAY_IN_MILLIS -> "Ontem"
        else -> java.text.SimpleDateFormat("d 'de' MMMM", java.util.Locale("pt", "BR")).format(java.util.Date(at))
    }
}

fun timeLabel(at: Long): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale("pt", "BR")).format(java.util.Date(at))
