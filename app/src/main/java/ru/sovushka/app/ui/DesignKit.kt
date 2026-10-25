package ru.sovushka.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* ======================= Цвета и иконки ======================= */

/** Пастельные заливки иконок и наклеек. */
object Pastel {
    val Pink = Color(0xFFFFC1DA)
    val Blue = Color(0xFFBFE3FF)
    val Yellow = Color(0xFFFFE08A)
    val Mint = Color(0xFFC9F2D6)
    val Lilac = Color(0xFFE5D4FF)
    val Orange = Color(0xFFFFC98B)
    val all = listOf(Pink, Blue, Yellow, Mint, Lilac)
}

/** Тёмно-сливовый контур иконок. */
val InkColor = Color(0xFF3D2C5C)

private fun svgPath(d: String): Path = PathParser().parsePathString(d).toPath()

/** Иконка в сетке 48×48: заливка (tint), белые блики, точки и контур. */
class IconSpec(fill: List<String>, accent: List<String>, dot: List<String>, line: List<String>, val tint: Color) {
    val fill = fill.map(::svgPath)
    val accent = accent.map(::svgPath)
    val dot = dot.map(::svgPath)
    val line = line.map(::svgPath)
}

private val iconStroke = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)

fun DrawScope.drawIcon(spec: IconSpec, sizePx: Float, tint: Color = spec.tint, ink: Color = InkColor) {
    val k = sizePx / 48f
    scale(k, k, pivot = Offset.Zero) {
        spec.fill.forEach { drawPath(it, tint) }
        spec.accent.forEach { drawPath(it, Color.White) }
        spec.dot.forEach { drawPath(it, ink) }
        spec.line.forEach { drawPath(it, ink, style = iconStroke) }
    }
}

@Composable
fun AppIcon(spec: IconSpec, modifier: Modifier = Modifier, size: Dp = 28.dp, tint: Color = spec.tint, ink: Color = InkColor) {
    Canvas(modifier.size(size)) { drawIcon(spec, this.size.minDimension, tint, ink) }
}

/** Эмодзи интерфейса → иконка набора (если для эмодзи есть своя иконка). */
fun iconFor(emoji: String): IconSpec? = when (emoji) {
    "📸", "📷" -> AppIcons.CAMERA
    "🔁", "🔄" -> AppIcons.REPEAT
    "📚" -> AppIcons.BOOKS
    "💬" -> AppIcons.CHAT
    "🎒" -> AppIcons.BACKPACK
    "🏫" -> AppIcons.SCHOOL
    "📘", "📝" -> AppIcons.RULES
    "⭐" -> AppIcons.STAR
    "🔥" -> AppIcons.FLAME
    "🎤" -> AppIcons.MIC
    "🔊" -> AppIcons.SPEAKER
    "🐢" -> AppIcons.TURTLE
    "⚙️" -> AppIcons.GEAR
    "🏠" -> AppIcons.HOME
    "💡" -> AppIcons.BULB
    "✅", "💾", "👍" -> AppIcons.CHECK
    "🏆", "🎉" -> AppIcons.TROPHY
    "🖼" -> AppIcons.GALLERY
    "✍️", "✏️" -> AppIcons.PENCIL
    "✨" -> AppIcons.SPARKLE
    "▶️", "➡️" -> AppIcons.PLAY
    "🔍" -> AppIcons.SEARCH
    "💪", "💗" -> AppIcons.HEART
    "🔑" -> AppIcons.BELL
    else -> null
}

/* ======================= Живой фон ======================= */

private class Floater(val x: Float, val y: Float, val size: Float, val speed: Float, val phase: Float, val kind: Int, val color: Color)

private val floaters = Random(7).let { r ->
    List(13) {
        Floater(
            x = r.nextFloat(), y = r.nextFloat(), size = 10f + r.nextFloat() * 18f,
            speed = 0.5f + r.nextFloat() * 0.9f, phase = r.nextFloat(), kind = it % 3,
            color = listOf(Color(0xFFF48FB1), Color(0xFF90CAF9), Color(0xFFCE93D8), Color(0xFFFFE082))[it % 4],
        )
    }
}

/**
 * Розово-голубой фон с медленно плывущими пузырьками, звёздочками и искорками.
 * Рисуется на Canvas без размытий; время читается только в фазе отрисовки — без лишних перекомпоновок.
 */
@Composable
fun MagicBackground(content: @Composable BoxScope.() -> Unit) {
    val t by rememberInfiniteTransition(label = "bg").animateFloat(
        0f, 1f, infiniteRepeatable(tween(30_000, easing = LinearEasing)), label = "bgT",
    )
    Box(
        Modifier.fillMaxSize().background(Palette.background).drawBehind {
            floaters.forEach { f ->
                val y = ((f.y - t * f.speed) % 1f + 1f) % 1f * (size.height + 80f) - 40f
                val x = f.x * size.width + sin((t * f.speed * 6f + f.phase) * 2f * PI.toFloat()) * 18f
                val s = f.size * density / 2.2f
                when (f.kind) {
                    0 -> {
                        drawCircle(Color.White.copy(alpha = 0.28f), s, Offset(x, y))
                        drawCircle(Color.White.copy(alpha = 0.7f), s, Offset(x, y), style = Stroke(1.5f * density))
                        drawCircle(Color.White.copy(alpha = 0.8f), s * 0.22f, Offset(x - s * 0.35f, y - s * 0.35f))
                    }
                    1 -> translate(x - s, y - s) { scale(s * 2 / 48f, s * 2 / 48f, pivot = Offset.Zero) { AppIcons.STAR.fill.forEach { drawPath(it, f.color.copy(alpha = 0.35f)) } } }
                    else -> translate(x - s, y - s) { scale(s * 2 / 48f, s * 2 / 48f, pivot = Offset.Zero) { AppIcons.SPARKLE.fill.forEach { drawPath(it, f.color.copy(alpha = 0.4f)) } } }
                }
            }
        },
        content = content,
    )
}

/* ======================= Конфетные 3D-кнопки ======================= */

/** «Дно» кнопки — более тёмный тон под лицевой частью, даёт объём. */
fun baseColorFor(brush: Brush): Color = when {
    brush === Palette.pinkish -> Color(0xFFA2448F)
    brush === Palette.bluish -> Color(0xFF4A73C2)
    brush === Palette.buttonSoft -> Color(0xFFB27BB4)
    brush === Palette.button -> Color(0xFF8A4FB5)
    brush === WhiteCandy -> Color(0xFFD9CDE6)
    brush === GoodCandy -> Color(0xFF2E8A50)
    brush === BadCandy -> Color(0xFFB3324F)
    brush === DimCandy -> Color(0xFFD6CFE0)
    else -> Color(0xFF7B6A9B)
}

/**
 * Объёмная кнопка-конфета: при нажатии лицевая часть «вдавливается», телефон слегка вибрирует,
 * сверху — глянцевый блик. appear ≥ 0 — кнопка появляется волной (задержка по номеру).
 */
@Composable
fun CandyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = Palette.button,
    shape: Shape = RoundedCornerShape(26.dp),
    depth: Dp = 6.dp,
    enabled: Boolean = true,
    appear: Int = -1,
    fillWidth: Boolean = false,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed && enabled) 1f else 0f, spring(stiffness = 900f), label = "press")
    val haptic = LocalHapticFeedback.current
    val shown = remember { Animatable(if (appear >= 0) 0f else 1f) }
    if (appear >= 0) LaunchedEffect(Unit) { delay(appear * 70L); shown.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
    val face = if (enabled) brush else Brush.linearGradient(listOf(Color(0xFFCFC8DA), Color(0xFFB9B1C7)))
    val base = if (enabled) baseColorFor(brush) else Color(0xFF9C93AC)
    Box(
        modifier
            .graphicsLayer {
                alpha = shown.value.coerceIn(0f, 1f)
                translationY = (1f - shown.value) * 40.dp.toPx()
                val s = 1f - press * 0.02f
                scaleX = s; scaleY = s
            }
            .padding(bottom = depth),
    ) {
        Row(
            (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .offset(y = depth * press)
                .drawBehind {
                    // «Дно» конфеты — та же форма, сдвинутая вниз; при нажатии лицевая часть опускается на него.
                    val outline = shape.createOutline(size, layoutDirection, this)
                    translate(0f, depth.toPx() * (1f - press)) { drawOutline(outline, base) }
                }
                .clip(shape)
                .background(face)
                .drawBehind {
                    // Глянцевый блик сверху.
                    val inset = 10.dp.toPx()
                    drawRoundRect(
                        Color.White.copy(alpha = 0.22f), Offset(inset, 4.dp.toPx()),
                        Size(size.width - inset * 2, size.height * 0.36f), CornerRadius(size.height * 0.2f),
                    )
                }
                .clickable(interaction, indication = null, enabled = enabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** Белый кружок-«фишка» с иконкой — для больших кнопок. */
@Composable
fun IconChip(spec: IconSpec?, emoji: String, size: Dp = 52.dp) {
    Box(
        Modifier.size(size).shadow(3.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        if (spec != null) AppIcon(spec, size = size * 0.72f) else Text(emoji, fontSize = (size.value * 0.55f).sp)
    }
}

/* ======================= Эффекты ======================= */

private class Particle(val angle: Float, val speed: Float, val size: Float, val spin: Float, val color: Color, val shape: Int)

/**
 * Взрыв конфетти (или звёздочек) из точки origin (доли экрана). Позиции частиц считаются по формуле
 * от времени — без состояния на каждую частицу, поэтому дёшево даже на простых телефонах.
 */
@Composable
fun ConfettiBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
    origin: Offset = Offset(0.5f, 0.35f),
    count: Int = 70,
    stars: Boolean = false,
    durationMs: Int = 1800,
) {
    val progress = remember { Animatable(1f) }
    val parts = remember(trigger) {
        val r = Random(trigger * 31 + 7)
        val colors = listOf(Color(0xFFEC5F9C), Color(0xFF4A9FE8), Color(0xFFFFC94D), Color(0xFF7ED9A0), Color(0xFFB388FF))
        List(count) {
            Particle(
                angle = r.nextFloat() * 2f * PI.toFloat(),
                speed = 0.5f + r.nextFloat(),
                size = if (stars) 14f + r.nextFloat() * 12f else 6f + r.nextFloat() * 7f,
                spin = (r.nextFloat() - 0.5f) * 720f,
                color = colors[it % colors.size],
                shape = if (stars) 2 else it % 3,
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val ox = size.width * origin.x
        val oy = size.height * origin.y
        val time = p * durationMs / 1000f
        val power = (if (stars) 380f else 620f) * density / 2.6f
        val gravity = 900f * density / 2.6f
        val alpha = (1f - p * p).coerceIn(0f, 1f)
        parts.forEach { q ->
            val x = ox + cos(q.angle) * q.speed * power * time
            val y = oy + sin(q.angle) * q.speed * power * time - 260f * density / 2.6f * time + 0.5f * gravity * time * time
            val s = q.size * density / 2.2f
            rotate(q.spin * time, pivot = Offset(x, y)) {
                when (q.shape) {
                    0 -> drawRect(q.color.copy(alpha = alpha), Offset(x - s / 2, y - s / 4), Size(s, s / 2))
                    1 -> drawCircle(q.color.copy(alpha = alpha), s / 2.5f, Offset(x, y))
                    else -> translate(x - s / 2, y - s / 2) {
                        scale(s / 48f, s / 48f, pivot = Offset.Zero) { AppIcons.STAR.fill.forEach { drawPath(it, q.color.copy(alpha = alpha)) } }
                    }
                }
            }
        }
    }
}

/** Встряхивание при ошибке: увеличьте trigger — элемент дрогнет. */
fun Modifier.shake(trigger: Int): Modifier = composed {
    val dx = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) for (v in listOf(14f, -12f, 9f, -6f, 3f, 0f)) dx.animateTo(v, tween(45))
    }
    graphicsLayer { translationX = dx.value * density }
}

/** Картинка к слову — как наклейка: белая обводка, тень, лёгкий наклон. */
@Composable
fun Sticker(emoji: String, size: Dp, seed: Int = emoji.hashCode(), modifier: Modifier = Modifier) {
    val tilt = ((seed % 7 + 7) % 7 - 3) * 1.6f
    val tone = Pastel.all[(seed % Pastel.all.size + Pastel.all.size) % Pastel.all.size]
    Box(
        modifier.size(size).rotate(tilt).shadow(8.dp, RoundedCornerShape(size * 0.28f))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color.White)
            .padding(size * 0.06f)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(Brush.linearGradient(listOf(Color.White, tone))),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = (size.value * 0.56f).sp) }
}

/** Прогресс-бар с переливом и звёздочкой на конце. */
@Composable
fun CandyProgress(progress: Float, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), spring(stiffness = 120f), label = "progress")
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "sh",
    )
    Box(modifier.fillMaxWidth().height(22.dp)) {
        Canvas(Modifier.fillMaxWidth().height(14.dp).align(Alignment.CenterStart)) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(Color.White.copy(alpha = 0.85f), cornerRadius = r)
            val w = size.width * p
            if (w > 1f) {
                drawRoundRect(Palette.button, size = Size(w, size.height), cornerRadius = r)
                // Перелив: светлая полоса бежит по заполненной части.
                val sx = (shimmer * 1.4f - 0.2f) * w
                drawRoundRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent), sx - 40f, sx + 40f),
                    size = Size(w, size.height), cornerRadius = r,
                )
                drawRoundRect(Color.White.copy(alpha = 0.3f), Offset(6f, 3f), Size((w - 12f).coerceAtLeast(0f), size.height * 0.3f), r)
            }
        }
        Canvas(Modifier.fillMaxHeight().fillMaxWidth()) {
            val s = size.height * 1.15f
            val x = (size.width * p - s / 2).coerceIn(0f, size.width - s)
            translate(x, (size.height - s) / 2) { drawIcon(AppIcons.STAR, s) }
        }
    }
}

/** Звёзды результата выпрыгивают по одной. */
@Composable
fun PopStars(count: Int, max: Int = 3, size: Dp = 64.dp) {
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        repeat(max) { i ->
            val s = remember { Animatable(0f) }
            LaunchedEffect(count) { delay(250L + i * 320L); s.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 260f)) }
            AppIcon(
                AppIcons.STAR,
                Modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; rotationZ = (1f - s.value) * -90f },
                size = if (i == 1) size * 1.2f else size,
                tint = if (i < count) Pastel.Yellow else Color(0xFFE6E0EE),
            )
        }
    }
}

/** Бейдж со счётчиком (звёзды, серия дней). flicker — огонёк «пляшет». */
@Composable
fun CounterBadge(spec: IconSpec, value: Int, flicker: Boolean = false) {
    val f by rememberInfiniteTransition(label = "flick").animateFloat(
        0.92f, 1.08f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "f",
    )
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.9f))
            .border(2.dp, Color.White, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(spec, Modifier.graphicsLayer { if (flicker) { scaleX = f; scaleY = 2f - f } }, size = 24.dp)
        Text(" $value", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = InkColor)
    }
}
