package com.cozy.herbshop.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = SoftCream,
    primaryContainer = WarmSage,
    onPrimaryContainer = CozyEarthyBrown,
    secondary = TerracottaWarm,
    onSecondary = SoftCream,
    background = SoftCream,
    onBackground = CozyEarthyBrown,
    surface = CardCream,
    onSurface = CozyEarthyBrown,
    surfaceVariant = WarmBeige,
    onSurfaceVariant = CozyEarthyBrown,
    outline = CozyEarthyBrown,
    tertiary = SoftGold
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkCozyPrimary,
    onPrimary = DarkCozyBackground,
    primaryContainer = DarkCozySurface,
    onPrimaryContainer = DarkCozyText,
    secondary = TerracottaWarm,
    onSecondary = SoftCream,
    background = DarkCozyBackground,
    onBackground = DarkCozyText,
    surface = DarkCozyCard,
    onSurface = DarkCozyText,
    surfaceVariant = DarkCozySurface,
    onSurfaceVariant = DarkCozyText,
    outline = DarkCozyText,
    tertiary = SoftGold
)

@Composable
fun CozyHerbShopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
