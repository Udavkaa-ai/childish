package ru.sovushka.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Палитра: розовый и голубой с градиентами. */
object Palette {
    val Pink = Color(0xFFEC5F9C)
    val PinkLight = Color(0xFFF8BBD9)
    val Lilac = Color(0xFFCE93D8)
    val Blue = Color(0xFF4A9FE8)
    val BlueLight = Color(0xFF81D4FA)
    val Ink = Color(0xFF2E2A4F)
    val Muted = Color(0xFF7A7499)
    val Good = Color(0xFF43A047)
    val Bad = Color(0xFFE5486F)
    val Card = Color(0xFFFFFFFF)

    val background = Brush.verticalGradient(listOf(Color(0xFFFFEAF4), Color(0xFFEAF4FF)))
    val button = Brush.linearGradient(listOf(Pink, Blue))
    val buttonSoft = Brush.linearGradient(listOf(Color(0xFFF48FB1), Color(0xFF90CAF9)))
    val pinkish = Brush.linearGradient(listOf(Color(0xFFF06BA8), Color(0xFFBA68C8)))
    val bluish = Brush.linearGradient(listOf(Color(0xFF7E8CE0), Color(0xFF4FC3F7)))
}

@Composable
fun SovushkaTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = Palette.Pink,
        secondary = Palette.Blue,
        background = Color(0xFFFFF5FA),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Palette.Ink,
        onSurface = Palette.Ink,
    )
    val base = Typography()
    val type = base.copy(
        headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink),
        titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Palette.Ink),
        titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink),
        bodyLarge = TextStyle(fontSize = 18.sp, color = Palette.Ink),
        bodyMedium = TextStyle(fontSize = 16.sp, color = Palette.Ink),
    )
    MaterialTheme(colorScheme = colors, typography = type, content = content)
}

@Composable
fun GradientScreen(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Palette.background), content = content)
}
