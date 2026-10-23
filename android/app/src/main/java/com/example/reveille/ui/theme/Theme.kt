package com.example.reveille.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CreativeTacticalColorScheme = lightColorScheme(
    primary = SignalRedVibrant,
    onPrimary = TacticalCardBg,
    primaryContainer = SignalRedDark,
    onPrimaryContainer = TacticalCardBg,
    secondary = TacticalForestGreen,
    onSecondary = TacticalCardBg,
    secondaryContainer = TacticalOliveCard,
    onSecondaryContainer = TextDarkTactical,
    background = TacticalLightBg,
    onBackground = TextDarkTactical,
    surface = TacticalCardBg,
    onSurface = TextDarkTactical,
    surfaceVariant = TacticalOliveCard,
    onSurfaceVariant = TextDarkTactical,
    outline = OliveBorderCreative
)

@Composable
fun ReveilleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CreativeTacticalColorScheme,
        typography = Typography,
        content = content
    )
}