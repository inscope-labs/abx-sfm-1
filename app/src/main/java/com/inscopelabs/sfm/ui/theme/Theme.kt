package com.inscopelabs.sfm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val HighDensityColorScheme = lightColorScheme(
    primary = VaultEmeraldPrimary,
    onPrimary = VaultEmeraldOnPrimary,
    primaryContainer = VaultEmeraldContainer,
    onPrimaryContainer = VaultEmeraldOnContainer,
    secondary = VaultCyanSecondary,
    secondaryContainer = VaultCyanContainer,
    background = VaultDarkBackground,
    surface = VaultDarkSurface,
    surfaceVariant = VaultDarkSurfaceVariant,
    onBackground = VaultTextPrimary,
    onSurface = VaultTextPrimary,
    onSurfaceVariant = VaultTextSecondary,
    error = VaultError,
    outline = VaultDarkCardBorder
)

@Composable
fun SecureFilesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HighDensityColorScheme,
        typography = Typography,
        content = content
    )
}

// Keep backwards-compatibility alias for template references
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SecureFilesTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

