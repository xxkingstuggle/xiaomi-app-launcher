package com.tencent.mobileqq.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4866A8),
    onPrimary = Color.White,
    secondary = Color(0xFF5D6070),
    surface = Color(0xFFF9F9FF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C6F4),
    onPrimary = Color(0xFF1D315E),
    secondary = Color(0xFFC5C6D0),
    surface = Color(0xFF111318)
)

@Composable
fun AppCenterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
