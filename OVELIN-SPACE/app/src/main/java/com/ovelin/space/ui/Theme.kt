package com.ovelin.space.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpaceColors = darkColorScheme(
    primary = Color(0xFF54D8C2),
    onPrimary = Color(0xFF06241F),
    secondary = Color(0xFF8AA7FF),
    onSecondary = Color(0xFF101B3B),
    background = Color(0xFF0A0D13),
    onBackground = Color(0xFFF2F5FA),
    surface = Color(0xFF131923),
    onSurface = Color(0xFFF2F5FA),
    surfaceVariant = Color(0xFF202936),
    onSurfaceVariant = Color(0xFFB5C0CF),
    outline = Color(0xFF303B49),
    error = Color(0xFFFF777A),
)

@Composable
fun OvelinSpaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpaceColors,
        content = content,
    )
}
