package ru.sovushka.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import ru.sovushka.app.R
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

/** Общие «часы» экрана для живых бликов на стекле: одна анимация на экран, читается только при отрисовке. */
val LocalGlassClock = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(0f) }

private class Floater(val x: Float, val y: Float, val size: Float, val speed: Float, val phase: Float, val kind: Int, val color: Color)

private val floaters = Random(7).let { r ->
    List(11) {
        Floater(
            x = r.nextFloat(), y = r.nextFloat(), size = 10f + r.nextFloat() * 18f,
            speed = 0.5f + r.nextFloat() * 0.9f, phase = r.nextFloat(), kind = it % 3,
            color = listOf(Color(0xFFF48FB1), Color(0xFF90CAF9), Color(0xFFCE93D8), Color(0xFFFFE082))[it % 4],
        )
    }
}

/** Цветные пятна «северного сияния»: медленно плывут и дышат поверх картинки неба. */
private class Glow(val x: Float, val y: Float, val r: Float, val speed: Int, val phase: Float, val color: Color)

private val glows = listOf(
    Glow(0.12f, 0.18f, 0.62f, 1, 0.00f, Color(0xFFFF7EB9)),
    Glow(0.88f, 0.34f, 0.66f, 1, 0.33f, Color(0xFF6EC1FF)),
    Glow(0.28f, 0.70f, 0.70f, 2, 0.60f, Color(0xFFB98BFF)),
    Glow(0.82f, 0.92f, 0.56f, 1, 0.15f, Color(0xFF7FE8D8)),
)

/** Картинка неба (нарисована в OpenArt) — декодируется один раз на всё приложение, 16 бит на точку. */
private var skyCache: ImageBitmap? = null

@Composable
private fun rememberSky(): ImageBitmap? {
    val res = LocalContext.current.resources
    return remember {
        skyCache ?: runCatching {
            val o = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
            BitmapFactory.decodeResource(res, R.drawable.bg_sky, o).asImageBitmap()
        }.getOrNull().also { skyCache = it }
    }
}

/**
 * Фон всех экранов: небо с облаками, поверх — переливающиеся пятна света и плывущие пузырьки со звёздочками.
 * Всё рисуется на Canvas без размытий; время читается только в фазе отрисовки — без лишних перекомпоновок.
 */
@Composable
fun MagicBackground(content: @Composable BoxScope.() -> Unit) {
    val clock = rememberInfiniteTransition(label = "bg").animateFloat(
        0f, 1f, infiniteRepeatable(tween(30_000, easing = LinearEasing)), label = "bgT",
    )
    val sky = rememberSky()
    CompositionLocalProvider(LocalGlassClock provides clock) {
        Box(
            Modifier.fillMaxSize().background(Palette.background).drawBehind {
                val t = clock.value
                if (sky != null) {
                    // Как ContentScale.Crop: заполняем экран, лишнее по краям обрезается.
                    val k = maxOf(size.width / sky.width, size.height / sky.height)
                    val w = (sky.width * k).toInt()
                    val h = (sky.height * k).toInt()
                    drawImage(
                        sky, dstOffset = IntOffset(((size.width - w) / 2).toInt(), ((size.height - h) / 2).toInt()),
                        dstSize = IntSize(w, h), filterQuality = FilterQuality.Low,
                    )
                }
                glows.forEach { g ->
                    val a = 2f * PI.toFloat() * (t * g.speed + g.phase)
                    val c = Offset((g.x + 0.14f * sin(a)) * size.width, (g.y + 0.07f * cos(a * 1.5f)) * size.height)
                    val r = g.r * size.width * (1f + 0.15f * sin(a * 2f))
                    drawCircle(Brush.radialGradient(listOf(g.color.copy(alpha = 0.34f), g.color.copy(alpha = 0f)), c, r), r, c)
                }
                floaters.forEach { f ->
                    val y = ((f.y - t * f.speed) % 1f + 1f) % 1f * (size.height + 80f) - 40f
                    val x = f.x * size.width + sin((t * f.speed * 6f + f.phase) * 2f * PI.toFloat()) * 18f
                    val s = f.size * density / 2.2f
                    when (f.kind) {
                        0 -> {
                            drawCircle(Color.White.copy(alpha = 0.22f), s, Offset(x, y))
                            drawCircle(Color.White.copy(alpha = 0.75f), s, Offset(x, y), style = Stroke(1.5f * density))
                            drawCircle(Color.White.copy(alpha = 0.85f), s * 0.22f, Offset(x - s * 0.35f, y - s * 0.35f))
                        }
                        1 -> translate(x - s, y - s) { scale(s * 2 / 48f, s * 2 / 48f, pivot = Offset.Zero) { AppIcons.STAR.fill.forEach { drawPath(it, f.color.copy(alpha = 0.45f)) } } }
                        else -> translate(x - s, y - s) { scale(s * 2 / 48f, s * 2 / 48f, pivot = Offset.Zero) { AppIcons.SPARKLE.fill.forEach { drawPath(it, Color.White.copy(alpha = 0.8f)) } } }
                    }
                }
            },
            content = content,
        )
    }
}

/* ======================= Стекло ======================= */

/** Светлая рамка стекла: ярче сверху-слева, где «падает свет». */
val GlassRim: Brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.7f)))

/** Тень под светлым текстом на цветном стекле — чтобы читался на любом фоне. */
val GlassTextShadow = Shadow(Color(0x77402060), Offset(0f, 2f), 5f)

/** Блики стекла: светлая верхняя половина и отражение снизу. */
private fun DrawScope.drawGloss() {
    drawRect(
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.5f), 0.46f to Color.White.copy(alpha = 0.1f),
            0.5f to Color.Transparent, 0.8f to Color.Transparent, 1f to Color.White.copy(alpha = 0.22f),
        ),
    )
}

/** Стеклянная подложка: полупрозрачная заливка, блики и светлая рамка. Без размытия — дёшево. */
fun Modifier.glass(shape: Shape, tint: Color = Color.White.copy(alpha = 0.62f), rim: Dp = 1.5.dp): Modifier =
    this.clip(shape).background(tint).drawBehind { drawGloss() }.border(rim, GlassRim, shape)

/** Цвет мягкого ореола вокруг стеклянной кнопки. */
fun glowFor(brush: Brush): Color = when {
    brush === Palette.pinkish -> Color(0xFFE05AA8)
    brush === Palette.bluish -> Color(0xFF4F8FE0)
    brush === Palette.buttonSoft -> Color(0xFFC98BD0)
    brush === Palette.button -> Color(0xFFA25BD0)
    brush === GlassGood -> Color(0xFF2E9A58)
    brush === GlassBad -> Color(0xFFD03A62)
    else -> Color.White
}

private val GlassDisabled: Brush = Brush.linearGradient(listOf(Color(0x88CFC8DA), Color(0x88B9B1C7)))

/**
 * Стеклянная кнопка: цветное полупрозрачное стекло с бликом и светлой рамкой, по нему время от времени
 * пробегает отсвет. При нажатии стекло светлеет и чуть сжимается, телефон слегка вибрирует.
 * appear ≥ 0 — кнопка появляется волной (задержка по номеру). depth — насколько ореол уходит вниз.
 */
@Composable
fun GlassButton(
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
    val clock = LocalGlassClock.current
    val seed = remember { Random.nextFloat() }
    val shown = remember { Animatable(if (appear >= 0) 0f else 1f) }
    if (appear >= 0) LaunchedEffect(Unit) { delay(appear * 70L); shown.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
    val face = if (enabled) brush else GlassDisabled
    val glow = if (enabled) glowFor(brush) else Color(0xFF9C93AC)
    Box(
        modifier
            .graphicsLayer {
                alpha = shown.value.coerceIn(0f, 1f)
                translationY = (1f - shown.value) * 40.dp.toPx()
                val s = 1f - press * 0.04f
                scaleX = s; scaleY = s
            }
            .padding(bottom = depth / 2),
    ) {
        Row(
            (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .drawBehind {
                    // Мягкий ореол вокруг стекла: две полупрозрачные обводки, чуть смещённые вниз.
                    val outline = shape.createOutline(size, layoutDirection, this)
                    translate(0f, depth.toPx() * 0.45f * (1f - press)) {
                        drawOutline(outline, glow.copy(alpha = 0.16f), style = Stroke(9.dp.toPx()))
                        drawOutline(outline, glow.copy(alpha = 0.24f), style = Stroke(4.dp.toPx()))
                    }
                }
                .clip(shape)
                .background(face)
                .drawBehind {
                    drawGloss()
                    // Бегущий отсвет: раз в ~6 секунд косая светлая полоса проходит по стеклу.
                    val phase = (clock.value * 5f + seed) % 1f
                    if (phase < 0.2f) {
                        val x = -size.height + phase / 0.2f * (size.width + size.height * 2)
                        val band = 28.dp.toPx()
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                                start = Offset(x, 0f), end = Offset(x + band, band * 0.8f),
                            ),
                        )
                    }
                    if (press > 0f) drawRect(Color.White.copy(alpha = 0.2f * press))
                }
                .border(1.5.dp, GlassRim, shape)
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

/** Стеклянная «фишка» с иконкой — для больших кнопок. */
@Composable
fun IconChip(spec: IconSpec?, emoji: String, size: Dp = 52.dp) {
    Box(
        Modifier.size(size).glass(RoundedCornerShape(16.dp), Color.White.copy(alpha = 0.78f)),
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
fun GlassProgress(progress: Float, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), spring(stiffness = 120f), label = "progress")
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "sh",
    )
    Box(modifier.fillMaxWidth().height(22.dp)) {
        Canvas(Modifier.fillMaxWidth().height(14.dp).align(Alignment.CenterStart)) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(Color.White.copy(alpha = 0.5f), cornerRadius = r)
            drawRoundRect(Color.White, cornerRadius = r, style = Stroke(1.5f * density))
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
        Modifier.glass(RoundedCornerShape(50), Color.White.copy(alpha = 0.6f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(spec, Modifier.graphicsLayer { if (flicker) { scaleX = f; scaleY = 2f - f } }, size = 24.dp)
        Text(" $value", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = InkColor)
    }
}
