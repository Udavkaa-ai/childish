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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ru.sovushka.app.R
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
    /** Карточки — светлое полупрозрачное стекло. */
    val Card = Color(0xB8FFFFFF)

    val background = Brush.verticalGradient(listOf(Color(0xFFFFEAF4), Color(0xFFEAF4FF)))
    // Цветное стекло: те же розово-голубые градиенты, но полупрозрачные — сквозь них видно небо.
    val button = Brush.linearGradient(listOf(Pink.copy(alpha = 0.8f), Blue.copy(alpha = 0.8f)))
    val buttonSoft = Brush.linearGradient(listOf(Color(0xA8F06BA8), Color(0xA86FB4F0)))
    val pinkish = Brush.linearGradient(listOf(Color(0xCCF06BA8), Color(0xCCB063CC)))
    val bluish = Brush.linearGradient(listOf(Color(0xCC6F7FE0), Color(0xCC3FB5F0)))
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
    val f = Nunito
    val b = Typography()
    val type = Typography(
        displayLarge = b.displayLarge.copy(fontFamily = f), displayMedium = b.displayMedium.copy(fontFamily = f),
        displaySmall = b.displaySmall.copy(fontFamily = f), headlineLarge = b.headlineLarge.copy(fontFamily = f),
        headlineMedium = TextStyle(fontFamily = f, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Palette.Ink),
        headlineSmall = b.headlineSmall.copy(fontFamily = f),
        titleLarge = TextStyle(fontFamily = f, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink),
        titleMedium = TextStyle(fontFamily = f, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink),
        titleSmall = b.titleSmall.copy(fontFamily = f),
        bodyLarge = TextStyle(fontFamily = f, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink),
        bodyMedium = TextStyle(fontFamily = f, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Palette.Ink),
        bodySmall = b.bodySmall.copy(fontFamily = f),
        labelLarge = b.labelLarge.copy(fontFamily = f, fontWeight = FontWeight.Bold),
        labelMedium = b.labelMedium.copy(fontFamily = f), labelSmall = b.labelSmall.copy(fontFamily = f),
    )
    MaterialTheme(colorScheme = colors, typography = type, content = content)
}

/** Округлый детский шрифт Nunito (OFL), с кириллицей. */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

/** Фон всех экранов: небо, переливы света и плывущие пузырьки со звёздочками. */
@Composable
fun GradientScreen(content: @Composable BoxScope.() -> Unit) = MagicBackground(content)
