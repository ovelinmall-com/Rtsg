package com.ovelin.space.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand red sampled directly from the Iskander app icon's glow (~#A50000-#AB0000).
val IskanderRed = Color(0xFFB3121A)
val IskanderRedBright = Color(0xFFE2302A)
val IskanderRedDeep = Color(0xFF7A0E12)
val IskanderEmber = Color(0xFFFF8A3D)
val IskanderSmoke = Color(0xFF2B1012)

private val SpaceColors = darkColorScheme(
    primary = IskanderRedBright,
    onPrimary = Color(0xFF2B0304),
    secondary = IskanderEmber,
    onSecondary = Color(0xFF2B1400),
    background = Color(0xFF0D0405),
    onBackground = Color(0xFFF5E9E8),
    surface = Color(0xFF170809),
    onSurface = Color(0xFFF5E9E8),
    surfaceVariant = Color(0xFF26100F),
    onSurfaceVariant = Color(0xFFD9B3B1),
    outline = Color(0xFF4A1F1E),
    error = Color(0xFFFFB74D),
    onError = Color(0xFF2B1400),
)

@Composable
fun OvelinSpaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpaceColors,
        content = content,
    )
}
