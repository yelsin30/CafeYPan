package com.example.cafeypan.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CafePrimaryDark,
    secondary = CafeSecondaryDark,
    tertiary = Pink80,
    background = CafeBackgroundDark,
    surface = CafeSurfaceDark,
    onPrimary = Color(0xFF1B120C), // Espresso dark on light-primary text
    onSecondary = Color(0xFF1B120C),
    onBackground = CafeTextDarkDark,
    onSurface = CafeTextDarkDark
)

private val LightColorScheme = lightColorScheme(
    primary = CafePrimary,
    secondary = CafeSecondary,
    tertiary = Pink40,
    background = CafeBackground,
    surface = CafeSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = CafeTextDark,
    onSurface = CafeTextDark
)

@Composable
fun CafeYPanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false by default to preserve the premium custom brand theme
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
