package ru.sovushka.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.sovushka.app.data.Prefs
import ru.sovushka.app.graph

/** Большая стеклянная кнопка с иконкой — основной элемент навигации. */
@Composable
fun BigButton(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    brush: Brush = accentBrush,
    enabled: Boolean = true,
    appear: Int = -1,
    onClick: () -> Unit,
) {
    GlassButton(onClick, modifier.fillMaxWidth(), brush = brush, enabled = enabled, appear = appear, fillWidth = true) {
        IconChip(iconFor(emoji), emoji)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black, style = TextStyle(shadow = GlassTextShadow))
            if (subtitle != null) Text(subtitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, style = TextStyle(shadow = GlassTextShadow))
        }
    }
}

/** Обычная кнопка поменьше. */
@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, brush: Brush = accentBrush, enabled: Boolean = true, onClick: () -> Unit) {
    // Если кнопке задали ширину (weight, fillMaxWidth) — растягиваем лицевую часть, иначе кнопка по размеру текста.
    val fill = modifier != Modifier
    GlassButton(
        onClick, modifier, brush = brush, shape = RoundedCornerShape(50), depth = 4.dp, enabled = enabled, fillWidth = fill,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 11.dp),
    ) {
        Text(
            text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
            style = TextStyle(shadow = GlassTextShadow),
            modifier = if (fill) Modifier.weight(1f) else Modifier,
        )
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            GlassButton(
                onBack, brush = GlassWhite, shape = CircleShape, depth = 4.dp,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(9.dp),
            ) { AppIcon(AppIcons.BACK, size = 30.dp) }
            Spacer(Modifier.width(10.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (LocalTalkToggle.current) TalkToggle()
        trailing()
    }
}

/** Показывать ли в верхней панели кнопку «сколько говорит Ника». */
val LocalTalkToggle = staticCompositionLocalOf { false }

/**
 * Кнопка «сколько говорит Ника»: 🔊 всё — 🔉 меньше слов (только задания) — 🔇 молчит (только по кнопкам звука).
 * Нажатие переключает по кругу, режим запоминается.
 */
@Composable
fun TalkToggle() {
    val ctx = LocalContext.current
    val g = ctx.graph
    val prefs by g.settings.prefs.collectAsState()
    val (icon, label) = when (prefs.talk) {
        Prefs.TALK_TASKS -> "🔉" to "меньше слов"
        Prefs.TALK_SILENT -> "🔇" to "молчу"
        else -> "🔊" to ""
    }
    Row(
        Modifier.padding(end = 6.dp).glass(RoundedCornerShape(50), Color.White.copy(alpha = 0.7f), rim = 2.dp)
            .clickable {
                val next = when (prefs.talk) {
                    Prefs.TALK_FULL -> Prefs.TALK_TASKS
                    Prefs.TALK_TASKS -> Prefs.TALK_SILENT
                    else -> Prefs.TALK_FULL
                }
                g.settings.update { it.copy(talk = next) }
                if (next != Prefs.TALK_FULL) g.speaker.stop()
                val msg = when (next) {
                    Prefs.TALK_TASKS -> "Меньше слов: Ника читает только задания"
                    Prefs.TALK_SILENT -> "Ника молчит: звук — только по кнопкам 🔊"
                    else -> "Ника снова говорит всё"
                }
                android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 20.sp)
        if (label.isNotEmpty()) Text(" $label", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
    }
}

/** Ника с облачком-репликой. */
@Composable
fun NikaSays(text: String, mood: NikaMood, talking: Boolean, modifier: Modifier = Modifier, nikaHeight: Dp = 170.dp) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Nika(mood = mood, talking = talking, modifier = Modifier.height(nikaHeight))
        Spacer(Modifier.width(8.dp))
        if (text.isNotBlank()) {
            Box(Modifier.weight(1f)) {
                // Хвостик облачка в сторону Ники.
                androidx.compose.foundation.Canvas(Modifier.size(18.dp, 22.dp).align(Alignment.CenterStart).offset(x = (-9).dp)) {
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width, 0f); lineTo(0f, size.height / 2); lineTo(size.width, size.height); close()
                    }
                    drawPath(p, Color.White.copy(alpha = 0.86f))
                }
                Box(
                    Modifier
                        .glass(RoundedCornerShape(22.dp), Color.White.copy(alpha = 0.86f), rim = 2.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) { Text(text, style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}

/** Карточка-подложка из светлого стекла. */
@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(22.dp), Color.White.copy(alpha = 0.74f), rim = 2.dp)
            .padding(16.dp),
    ) { content() }
}

enum class ChoiceState { NORMAL, RIGHT, WRONG, DIM }

@Composable
fun ChoiceButton(text: String, state: ChoiceState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val brush = when (state) {
        ChoiceState.RIGHT -> GlassGood
        ChoiceState.WRONG -> GlassBad
        ChoiceState.DIM -> GlassDim
        ChoiceState.NORMAL -> GlassWhite
    }
    val fg = if (state == ChoiceState.RIGHT || state == ChoiceState.WRONG) Color.White else Palette.Ink
    GlassButton(
        { if (state == ChoiceState.NORMAL) onClick() }, modifier.fillMaxWidth(), brush = brush, shape = RoundedCornerShape(20.dp), depth = 5.dp, fillWidth = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 15.dp),
    ) {
        when (state) {
            ChoiceState.RIGHT -> AppIcon(AppIcons.CHECK, size = 28.dp, tint = Color.White)
            ChoiceState.WRONG -> AppIcon(AppIcons.CROSS, size = 28.dp, tint = Color.White)
            else -> Spacer(Modifier.width(28.dp))
        }
        Text(text, color = fg, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(28.dp))
    }
}

/** Круглая кнопка микрофона: пока слушает — пульсирует и расходится волнами. */
@Composable
fun MicButton(listening: Boolean, level: Float, size: Dp = 96.dp, label: String = "🎤", onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "mic")
    val p by pulse.animateFloat(1f, 1.1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "p")
    val wave by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1200)), label = "w")
    val scale = if (listening) p + level * 0.12f else 1f
    Box(
        Modifier.size(size * 1.35f).drawBehind {
            if (listening) {
                for (k in 0..1) {
                    val w = (wave + k * 0.5f) % 1f
                    drawCircle(Palette.Pink.copy(alpha = (1f - w) * 0.45f), radius = size.toPx() / 2 * (1f + w * 0.45f))
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        GlassButton(
            onClick, Modifier.scale(scale), brush = if (listening) GlassBad else Palette.button, shape = CircleShape, depth = 6.dp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(size * 0.2f),
        ) {
            val spec = iconFor(label)
            if (spec != null) AppIcon(spec, size = size * 0.6f, tint = Color.White) else Text(label, fontSize = (size.value * 0.42f).sp)
        }
    }
}

@Composable
fun Stars(count: Int, max: Int = 3, size: Int = 44) = PopStars(count, max, size.dp)

@Composable
fun SoundButton(onClick: () -> Unit, slow: Boolean = false) {
    GlassButton(
        onClick, brush = if (LocalMathStyle.current) (if (slow) MathPalette.teal else MathPalette.primary) else if (slow) Palette.bluish else Palette.pinkish,
        shape = CircleShape, depth = 4.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
    ) { AppIcon(if (slow) AppIcons.TURTLE else AppIcons.SPEAKER, size = 34.dp, tint = Color.White) }
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier.widthIn(max = 600.dp), color = Palette.Muted, fontSize = 14.sp)
}

/** Аватарка ребёнка: фото в кружке, а если фото нет — эмодзи. */
@Composable
fun KidAvatar(older: Boolean, emoji: String, size: Dp, modifier: Modifier = Modifier) {
    val g = LocalContext.current.graph
    val prefs by g.settings.prefs.collectAsState()
    val bmp by produceState<android.graphics.Bitmap?>(null, older, prefs.avatarStamp) {
        value = withContext(Dispatchers.IO) {
            val f = g.avatarFile(older)
            if (f.exists()) BitmapFactory.decodeFile(f.path) else null
        }
    }
    Box(
        modifier.size(size).shadow(4.dp, CircleShape).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)).border(2.dp, GlassRim, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val b = bmp
        if (b != null) {
            Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(emoji, fontSize = (size.value * 0.6f).sp)
        }
    }
}

/** Стёкла для кнопок-ответов и светлых кнопок. */
val GlassWhite: Brush = Brush.verticalGradient(listOf(Color(0xD9FFFFFF), Color(0x9EFFFFFF)))
val GlassGood: Brush = Brush.verticalGradient(listOf(Color(0xE06BD48F), Color(0xE03FB56C)))
val GlassBad: Brush = Brush.verticalGradient(listOf(Color(0xE0FF7E9D), Color(0xE0E5486F)))
val GlassDim: Brush = Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x4DFFFFFF)))
