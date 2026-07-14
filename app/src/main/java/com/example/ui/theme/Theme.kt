package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorSchemeBase = darkColorScheme(
    primary = HighDensityPrimaryDark,
    onPrimary = HighDensityOnPrimaryDark,
    primaryContainer = HighDensityPrimaryContainerDark,
    onPrimaryContainer = HighDensityOnPrimaryContainerDark,
    secondary = HighDensitySecondaryDark,
    onSecondary = HighDensityOnSecondaryDark,
    secondaryContainer = HighDensitySecondaryContainerDark,
    onSecondaryContainer = HighDensityOnSecondaryContainerDark,
    tertiary = Color(0xFFEFB8C8),
    background = HighDensityBackgroundDark,
    surface = HighDensitySurfaceDark,
    onBackground = HighDensityOnBackgroundDark,
    onSurface = HighDensityOnSurfaceDark,
    surfaceVariant = HighDensitySurfaceVariantDark,
    onSurfaceVariant = HighDensityOnSurfaceVariantDark,
    outline = HighDensityOutlineDark
)

private val LightColorSchemeBase = lightColorScheme(
    primary = HighDensityPrimaryLight,
    onPrimary = HighDensityOnPrimaryLight,
    primaryContainer = HighDensityPrimaryContainerLight,
    onPrimaryContainer = HighDensityOnPrimaryContainerLight,
    secondary = HighDensitySecondaryLight,
    onSecondary = HighDensityOnSecondaryLight,
    secondaryContainer = HighDensitySecondaryContainerLight,
    onSecondaryContainer = HighDensityOnSecondaryContainerLight,
    tertiary = Color(0xFF7D5260),
    background = HighDensityBackgroundLight,
    surface = HighDensitySurfaceLight,
    onBackground = HighDensityOnBackgroundLight,
    onSurface = HighDensityOnSurfaceLight,
    surfaceVariant = HighDensitySurfaceVariantLight,
    onSurfaceVariant = HighDensityOnSurfaceVariantLight,
    outline = HighDensityOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val dynamicPrimary = accentColor ?: Color(0xFF6750A4)

    val colorScheme = if (darkTheme) {
        DarkColorSchemeBase.copy(
            primary = dynamicPrimary,
            inversePrimary = dynamicPrimary.copy(alpha = 0.8f)
        )
    } else {
        LightColorSchemeBase.copy(
            primary = dynamicPrimary,
            inversePrimary = dynamicPrimary.copy(alpha = 0.8f)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
