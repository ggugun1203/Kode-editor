package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DroidStudioColorScheme = darkColorScheme(
    primary = BlueAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF094771),
    onPrimaryContainer = Color(0xFFCCE8FF),
    secondary = GreenAccent,
    onSecondary = Color(0xFF00382E),
    secondaryContainer = Color(0xFF005143),
    onSecondaryContainer = Color(0xFF70F7D7),
    tertiary = PurpleAccent,
    onTertiary = Color.White,
    background = VsCodeBg,
    onBackground = TextPrimary,
    surface = VsCodeSidebarBg,
    onSurface = TextPrimary,
    surfaceVariant = VsCodeHeaderBg,
    onSurfaceVariant = TextMuted,
    outline = GrayBorder,
    outlineVariant = Color(0xFF2A2D2E),
    error = RedAccent,
    onError = Color.White
)

@Composable
fun DroidStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DroidStudioColorScheme,
        typography = Typography,
        content = content
    )
}

// Keep backwards-compatible alias
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    DroidStudioTheme(content = content)
}
