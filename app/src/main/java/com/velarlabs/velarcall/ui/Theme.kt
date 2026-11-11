package com.velarlabs.velarcall.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object VelarColors {
    val Bg = Color(0xFF0B1220)
    val Surface = Color(0xFF121C30)
    val SurfaceHigh = Color(0xFF1A2742)
    val Teal = Color(0xFF2DD4BF)
    val TealDim = Color(0xFF134E4A)
    val Danger = Color(0xFFF87171)
    val DangerDim = Color(0xFF3B1A1F)
    val Amber = Color(0xFFFBBF24)
    val Text = Color(0xFFE6EDF7)
    val Muted = Color(0xFF8A98B3)
}

private val scheme = darkColorScheme(
    primary = VelarColors.Teal,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = VelarColors.TealDim,
    onPrimaryContainer = VelarColors.Teal,
    secondary = VelarColors.Muted,
    background = VelarColors.Bg,
    onBackground = VelarColors.Text,
    surface = VelarColors.Bg,
    onSurface = VelarColors.Text,
    surfaceVariant = VelarColors.Surface,
    onSurfaceVariant = VelarColors.Muted,
    surfaceContainer = VelarColors.Surface,
    surfaceContainerHigh = VelarColors.SurfaceHigh,
    error = VelarColors.Danger,
    errorContainer = VelarColors.DangerDim,
    outline = Color(0xFF2A3A5C),
)

@Composable
fun VelarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
