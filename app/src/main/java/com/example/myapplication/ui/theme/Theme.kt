package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OledColorScheme = darkColorScheme(
    primary = KurenaiRed,
    secondary = SumiGrey,
    tertiary = KurenaiRed,
    background = OledBlack,
    surface = SumiInk,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = WashiPaper,
    onSurface = WashiPaper,
    surfaceVariant = SumiGrey
)

private val DarkColorScheme = darkColorScheme(
    primary = KurenaiRed,
    secondary = SumiGrey,
    tertiary = KurenaiRed,
    background = androidx.compose.ui.graphics.Color(0xFF121212),
    surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = WashiPaper,
    onSurface = WashiPaper,
    surfaceVariant = SumiGrey
)

private val LightColorScheme = lightColorScheme(
    primary = KurenaiRed,
    secondary = SumiGrey,
    tertiary = KurenaiRed,
    background = WashiPaper,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = SumiInk,
    onSurface = SumiInk,
    surfaceVariant = SurfaceVariant
)

@Composable
fun MyApplicationTheme(
    themePreference: String = "LIGHT",
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to keep the minimalist theme
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themePreference) {
        "DARK", "OLED" -> true
        "LIGHT" -> false
        else -> isSystemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themePreference == "OLED" -> OledColorScheme
        themePreference == "DARK" -> DarkColorScheme
        themePreference == "LIGHT" -> LightColorScheme
        isSystemDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}