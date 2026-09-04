package com.aiclient.chat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Clay500,
    onPrimary = CreamSurface,
    primaryContainer = Clay500.copy(alpha = 0.14f),
    onPrimaryContainer = Clay700,
    secondary = Ink600,
    background = CreamBg,
    onBackground = Ink900,
    surface = CreamSurface,
    onSurface = Ink900,
    surfaceVariant = CreamSidebar,
    onSurfaceVariant = Ink600,
    outline = OutlineLight,
    error = ErrorRed,
)

private val DarkColors = darkColorScheme(
    primary = Clay500,
    onPrimary = CharcoalBg,
    primaryContainer = Clay500.copy(alpha = 0.22f),
    onPrimaryContainer = Bone100,
    secondary = Bone400,
    background = CharcoalBg,
    onBackground = Bone100,
    surface = CharcoalSurface,
    onSurface = Bone100,
    surfaceVariant = CharcoalSidebar,
    onSurfaceVariant = Bone400,
    outline = OutlineDark,
    error = ErrorRed,
)

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun AiClientChatTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDark -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
