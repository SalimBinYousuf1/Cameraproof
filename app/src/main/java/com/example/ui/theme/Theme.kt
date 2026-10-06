package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SalimSkyDark,
    onPrimary = SalimSkyOnPrimaryDark,
    primaryContainer = SalimSkyContainerDark,
    onPrimaryContainer = SalimSkyOnContainerDark,
    surface = SalimSurfaceDark,
    onSurface = SalimOnSurfaceDark,
    surfaceVariant = SalimSurfaceVariantDark,
    onSurfaceVariant = SalimOnSurfaceVariantDark,
    background = SalimBackgroundDark,
    onBackground = SalimOnSurfaceDark,
    outline = SalimOutlineDark,
    outlineVariant = SalimOutlineVariantDark,
    tertiary = SalimProofBadge,
    error = SalimError
)

private val LightColorScheme = lightColorScheme(
    primary = SalimSkyLight,
    onPrimary = SalimSkyOnPrimaryLight,
    primaryContainer = SalimSkyContainerLight,
    onPrimaryContainer = SalimSkyOnContainerLight,
    surface = SalimSurfaceLight,
    onSurface = SalimOnSurfaceLight,
    surfaceVariant = SalimSurfaceVariantLight,
    onSurfaceVariant = SalimOnSurfaceVariantLight,
    background = SalimBackgroundLight,
    onBackground = SalimOnSurfaceLight,
    outline = SalimOutlineLight,
    outlineVariant = SalimOutlineVariantLight,
    tertiary = SalimProofBadge,
    error = SalimError
)

@Composable
fun SalimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent calm branding across devices
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
