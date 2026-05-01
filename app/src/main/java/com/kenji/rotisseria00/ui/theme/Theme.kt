package com.kenji.rotisseria00.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val RotisseriaColorScheme = lightColorScheme(
    primary = PrimaryBrown,
    onPrimary = OnPrimary,
    secondary = SecondaryOrange,
    onSecondary = OnSecondary,
    background = BackgroundCream,
    surface = SurfaceWhite,
    onSurface = TextDarkBrown
)

@Composable
fun Rotisseria00Theme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = RotisseriaColorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = RotisseriaColorScheme,
        typography = Typography,
        content = content
    )
}