package com.example.fdmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.fdmanager.data.ThemeMode

// --- Brand: finance teal ---
private val LightColors = lightColorScheme(
    primary = Color(0xFF006A5F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9EF2E2),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4A635E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E1),
    onSecondaryContainer = Color(0xFF05201C),
    tertiary = Color(0xFF456179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCE5FF),
    onTertiaryContainer = Color(0xFF001E31),
    background = Color(0xFFF7FAF8),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFF7FAF8),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDAE5E0),
    onSurfaceVariant = Color(0xFF3F4946)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82D5C6),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005147),
    onPrimaryContainer = Color(0xFF9EF2E2),
    secondary = Color(0xFFB0CCC5),
    onSecondary = Color(0xFF1D3530),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFCCE8E1),
    tertiary = Color(0xFFADC9E6),
    onTertiary = Color(0xFF153349),
    tertiaryContainer = Color(0xFF2D4961),
    onTertiaryContainer = Color(0xFFCCE5FF),
    background = Color(0xFF101514),
    onBackground = Color(0xFFDEE4E1),
    surface = Color(0xFF101514),
    onSurface = Color(0xFFDEE4E1),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C4)
)

/** Semantic colors for FD status + maturity urgency, resolved per theme. */
data class StatusPalette(
    val active: Color, val onActive: Color, val activeContainer: Color,
    val matured: Color, val onMatured: Color, val maturedContainer: Color,
    val renewed: Color, val onRenewed: Color, val renewedContainer: Color
)

private val StatusLight = StatusPalette(
    active = Color(0xFF1B7A3D), onActive = Color(0xFFFFFFFF), activeContainer = Color(0xFFDDF3E2),
    matured = Color(0xFF9A6200), onMatured = Color(0xFFFFFFFF), maturedContainer = Color(0xFFFFE9C9),
    renewed = Color(0xFF1565C0), onRenewed = Color(0xFFFFFFFF), renewedContainer = Color(0xFFDCE9FF)
)

private val StatusDark = StatusPalette(
    active = Color(0xFF7ED99B), onActive = Color(0xFF0E2E1B), activeContainer = Color(0xFF144026),
    matured = Color(0xFFF5C477), onMatured = Color(0xFF3D2B00), maturedContainer = Color(0xFF4A3200),
    renewed = Color(0xFF9CC3FF), onRenewed = Color(0xFF0B2F5B), renewedContainer = Color(0xFF153A6E)
)

@Composable
fun statusPalette(dark: Boolean = isSystemInDarkTheme()): StatusPalette =
    if (dark) StatusDark else StatusLight

@Composable
fun FdManagerTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
