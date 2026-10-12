package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AstraColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = TitaniumElevated,
    onPrimaryContainer = IceBlue,
    secondary = IceBlue,
    onSecondary = ObsidianBlack,
    secondaryContainer = TitaniumSurface,
    onSecondaryContainer = PureWhite,
    tertiary = AmberAlert,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = PureWhite,
    surface = DarkGraphite,
    onSurface = PureWhite,
    surfaceVariant = TitaniumSurface,
    onSurfaceVariant = MetallicSilver,
    outline = TitaniumBorder,
    error = RedAlert,
    onError = PureWhite
)

@Composable
fun AstraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AstraColorScheme,
        typography = AstraTypography,
        content = content
    )
}
