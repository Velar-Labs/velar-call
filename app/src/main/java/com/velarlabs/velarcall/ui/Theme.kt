package com.velarlabs.velarcall.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Tokens de cor do VelarCall. Mesma estrutura no claro e no escuro. */
@Immutable
data class VelarPalette(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val outline: Color,
    val text: Color,
    val muted: Color,
    val brand: Color,
    val onBrand: Color,
    val brandSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val dark: Boolean,
)

val LightPalette = VelarPalette(
    bg = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEEF1F7),
    outline = Color(0xFFE3E7EF),
    text = Color(0xFF111827),
    muted = Color(0xFF6B7280),
    brand = Color(0xFF2F5BFF),
    onBrand = Color(0xFFFFFFFF),
    brandSoft = Color(0xFFE6ECFF),
    danger = Color(0xFFE5484D),
    dangerSoft = Color(0xFFFDECEC),
    success = Color(0xFF12A150),
    successSoft = Color(0xFFE5F6EC),
    warning = Color(0xFFD97706),
    warningSoft = Color(0xFFFEF3E2),
    dark = false,
)

val DarkPalette = VelarPalette(
    bg = Color(0xFF0E1015),
    surface = Color(0xFF171A21),
    surfaceAlt = Color(0xFF20242D),
    outline = Color(0xFF2A2F3A),
    text = Color(0xFFF3F4F6),
    muted = Color(0xFF9CA3AF),
    brand = Color(0xFF6F8BFF),
    onBrand = Color(0xFF0B1240),
    brandSoft = Color(0xFF1E2747),
    danger = Color(0xFFFF6B6E),
    dangerSoft = Color(0xFF3A1D20),
    success = Color(0xFF3DD68C),
    successSoft = Color(0xFF15301F),
    warning = Color(0xFFF5A524),
    warningSoft = Color(0xFF35290F),
    dark = true,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val palette: VelarPalette
    @Composable get() = LocalPalette.current

private val VelarType = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun VelarTheme(content: @Composable () -> Unit) {
    val p = if (isSystemInDarkTheme()) DarkPalette else LightPalette
    val scheme = if (p.dark) darkColorScheme(
        primary = p.brand, onPrimary = p.onBrand, primaryContainer = p.brandSoft, onPrimaryContainer = p.brand,
        background = p.bg, onBackground = p.text, surface = p.surface, onSurface = p.text,
        surfaceVariant = p.surfaceAlt, onSurfaceVariant = p.muted, surfaceContainer = p.surface,
        surfaceContainerHigh = p.surface, surfaceContainerLow = p.surface, surfaceContainerHighest = p.surfaceAlt,
        secondaryContainer = p.brandSoft, onSecondaryContainer = p.brand,
        error = p.danger, errorContainer = p.dangerSoft, outline = p.outline, outlineVariant = p.outline,
    ) else lightColorScheme(
        primary = p.brand, onPrimary = p.onBrand, primaryContainer = p.brandSoft, onPrimaryContainer = p.brand,
        background = p.bg, onBackground = p.text, surface = p.surface, onSurface = p.text,
        surfaceVariant = p.surfaceAlt, onSurfaceVariant = p.muted, surfaceContainer = p.surface,
        surfaceContainerHigh = p.surface, surfaceContainerLow = p.surface, surfaceContainerHighest = p.surfaceAlt,
        secondaryContainer = p.brandSoft, onSecondaryContainer = p.brand,
        error = p.danger, errorContainer = p.dangerSoft, outline = p.outline, outlineVariant = p.outline,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = VelarType) {
            // Cor padrão do texto fora de Scaffold/Surface (ex.: boas-vindas). Sem isso fica preto no tema escuro.
            CompositionLocalProvider(LocalContentColor provides p.text, content = content)
        }
    }
}
