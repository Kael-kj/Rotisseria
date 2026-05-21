package com.kenji.rotisseriaadmin.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val RotisseriaColorPalette = lightColors(
    primary = PrimaryBrown,
    onPrimary = OnPrimary,
    secondary = SecondaryOrange,
    onSecondary = OnSecondary,
    background = BackgroundCream,
    surface = SurfaceWhite,
    onSurface = TextDarkBrown
)

@Composable
fun Rotisseria00Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = RotisseriaColorPalette,
        typography = Typography,
        content = content
    )
}