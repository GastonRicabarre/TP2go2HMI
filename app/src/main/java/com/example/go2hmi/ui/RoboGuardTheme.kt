package com.example.go2hmi.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colores del design system RoboGuard (tomados del Figma). */
object RG {
    val Bg = Color(0xFF0A0E17)
    val Surface = Color(0xFF111A2B)
    val SurfaceAlt = Color(0xFF16213A)
    val Border = Color(0xFF1F2A3F)
    val Blue = Color(0xFF2563EB)
    val Cyan = Color(0xFF38BDF8)
    val Green = Color(0xFF10B981)
    val Amber = Color(0xFFF59E0B)
    val Red = Color(0xFFEF4444)
    val RedDark = Color(0xFF2A1518)
    val AmberDark = Color(0xFF2A2214)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
}

private val scheme = darkColorScheme(
    primary = RG.Blue,
    onPrimary = Color.White,
    secondary = RG.Cyan,
    background = RG.Bg,
    onBackground = Color.White,
    surface = RG.Surface,
    onSurface = Color.White,
    surfaceVariant = RG.Surface,
    onSurfaceVariant = RG.TextSecondary,
    error = RG.Red,
    outline = RG.Border
)

@Composable
fun RoboGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
