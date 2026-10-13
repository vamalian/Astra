package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.AccentTheme
import com.example.data.model.BackgroundIntensity

// Obsidian & Titanium Base
val ObsidianBlack = Color(0xFF04070D)
val VoidBlackColor = Color(0xFF000000)
val DarkNavyBase = Color(0xFF070D1A)
val TitaniumSurface = Color(0xFF0F1726)
val DarkGraphite = Color(0xFF0C131F)
val TitaniumElevated = Color(0xFF162238)
val TitaniumBorder = Color(0xFF1E304F)

// Accents
// Cyan Theme
val ElectricCyan = Color(0xFF00F0FF)
val ElectricCyanDark = Color(0xFF0098A6)
val IceBlue = Color(0xFF7DF9FF)
val NeonCyanGlow = Color(0x5500F0FF)

// Blue Theme
val PlasmaBlue = Color(0xFF2979FF)
val PlasmaBlueDark = Color(0xFF1565C0)
val PlasmaBlueLight = Color(0xFF82B1FF)
val PlasmaBlueGlow = Color(0x552979FF)

// Purple Theme
val NeonPurple = Color(0xFFB5179E)
val NeonPurpleDark = Color(0xFF7209B7)
val NeonPurpleLight = Color(0xFFE056FD)
val NeonPurpleGlow = Color(0x55B5179E)

// General Palette
val PureWhite = Color(0xFFFFFFFF)
val MetallicSilver = Color(0xFF94A3B8)
val DimSilver = Color(0xFF64748B)

// Alerts & States
val AmberAlert = Color(0xFFFFB020)
val AmberAlertGlow = Color(0x66FFB020)
val RedAlert = Color(0xFFFF3B30)
val RedAlertGlow = Color(0x66FF3B30)
val EmeraldActive = Color(0xFF10B981)
val EmeraldActiveGlow = Color(0x5510B981)

data class NexusThemePalette(
    val primary: Color,
    val primaryGlow: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color
)

fun getNexusPalette(accent: AccentTheme, bgIntensity: BackgroundIntensity): NexusThemePalette {
    val (primary, glow, light, dark) = when (accent) {
        AccentTheme.CYAN -> Quad(ElectricCyan, NeonCyanGlow, IceBlue, ElectricCyanDark)
        AccentTheme.BLUE -> Quad(PlasmaBlue, PlasmaBlueGlow, PlasmaBlueLight, PlasmaBlueDark)
        AccentTheme.PURPLE -> Quad(NeonPurple, NeonPurpleGlow, NeonPurpleLight, NeonPurpleDark)
    }

    val (bg, surface, surfaceElevated, border) = when (bgIntensity) {
        BackgroundIntensity.VOID_BLACK -> Quad(
            VoidBlackColor,
            Color(0xFF0A0A0A),
            Color(0xFF141414),
            Color(0xFF2A2A2A)
        )
        BackgroundIntensity.DEEP_NAVY -> Quad(
            DarkNavyBase,
            TitaniumSurface,
            TitaniumElevated,
            TitaniumBorder
        )
        BackgroundIntensity.CYBER_GRID -> Quad(
            ObsidianBlack,
            Color(0xFF0C1322),
            Color(0xFF142036),
            Color(0xFF1C2D4A)
        )
    }

    return NexusThemePalette(
        primary = primary,
        primaryGlow = glow,
        primaryLight = light,
        primaryDark = dark,
        background = bg,
        surface = surface,
        surfaceElevated = surfaceElevated,
        border = border
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
