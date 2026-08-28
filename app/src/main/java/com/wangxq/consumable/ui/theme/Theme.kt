package com.wangxq.consumable.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TealPrimary = Color(0xFF0F6E56)
val TealContainer = Color(0xFFE1F5EE)
val GreenStatus = Color(0xFF3B6D11)
val AmberStatus = Color(0xFFBA7517)
val RedStatus = Color(0xFFA32D2D)

private val LightScheme = lightColorScheme(
    primary = TealPrimary,
    primaryContainer = TealContainer,
    onPrimary = Color.White,
    background = Color(0xFFF5F5F4),
    surface = Color.White,
    onBackground = Color(0xFF1F2937),
    onSurface = Color(0xFF1F2937),
    secondary = Color(0xFF5F5E5A),
    onPrimaryContainer = Color(0xFF0F6E56),
    onSecondaryContainer = Color(0xFF5F5E5A)
)

@Composable
fun ConsumableTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightScheme, content = content)
}
