package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.model.AccentTheme
import com.example.data.model.BackgroundIntensity

val LocalNexusPalette = staticCompositionLocalOf {
    getNexusPalette(AccentTheme.CYAN, BackgroundIntensity.DEEP_NAVY)
}

@Composable
fun AstraNexusTheme(
    accentTheme: AccentTheme = AccentTheme.CYAN,
    backgroundIntensity: BackgroundIntensity = BackgroundIntensity.DEEP_NAVY,
    content: @Composable () -> Unit
) {
    val palette = getNexusPalette(accentTheme, backgroundIntensity)

    val colorScheme = darkColorScheme(
        primary = palette.primary,
        onPrimary = ObsidianBlack,
        primaryContainer = palette.surfaceElevated,
        onPrimaryContainer = palette.primaryLight,
        secondary = palette.primaryLight,
        onSecondary = ObsidianBlack,
        secondaryContainer = palette.surface,
        onSecondaryContainer = PureWhite,
        tertiary = AmberAlert,
        onTertiary = ObsidianBlack,
        background = palette.background,
        onBackground = PureWhite,
        surface = palette.surface,
        onSurface = PureWhite,
        surfaceVariant = palette.surfaceElevated,
        onSurfaceVariant = MetallicSilver,
        outline = palette.border,
        error = RedAlert,
        onError = PureWhite
    )

    CompositionLocalProvider(LocalNexusPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AstraTypography,
            content = content
        )
    }
}
