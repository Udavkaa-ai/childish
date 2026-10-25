package ru.sovushka.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Настроение Ники — от него зависит анимация. */
/** Наряд Ники: обычный (английский) и строгий (математика) — пиджак, галстук, очки. */
enum class NikaOutfit { CASUAL, STRICT }

val LocalNikaOutfit = staticCompositionLocalOf { NikaOutfit.CASUAL }

enum class NikaMood { IDLE, TALK, HAPPY, SAD, LISTEN, THINK, WAVE, DANCE, SURPRISED }

/** Мелкие «живые» движения, которые Ника делает сама, пока стоит или говорит. */
private enum class Quirk { NONE, LOOK_LEFT, LOOK_RIGHT, LOOK_UP, WINK, TILT, HOP, HAIR, WAVE, SHRUG, BOUNCE }

private val IDLE_QUIRKS = Quirk.entries.filter { it != Quirk.NONE }
private val TALK_QUIRKS = listOf(Quirk.TILT, Quirk.LOOK_LEFT, Quirk.LOOK_RIGHT, Quirk.SHRUG, Quirk.BOUNCE, Quirk.WINK)

/** Рука считается поднятой, если повёрнута сильнее этого угла: тогда её рисуем поверх головы. */
private const val RAISED = 60f

/**
 * Ника — персонаж, которого придумала Вероника. Нарисована векторно по её рисунку
 * (исходник — art/nika.svg, координаты в той же системе 200×320) и разложена на части,
 * чтобы двигать голову, глаза, рот, руки, волосы и всё тело.
 */
@Composable
fun NikaVector(mood: NikaMood, modifier: Modifier = Modifier, talking: Boolean = mood == NikaMood.TALK) {
    val loop = rememberInfiniteTransition(label = "nika")
    val phase by loop.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "breath",
    )
    val fast by loop.animateFloat(
        0f, 1f, infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart), label = "fast",
    )
    val beat by loop.animateFloat(
        0f, 1f, infiniteRepeatable(tween(950, easing = LinearEasing), RepeatMode.Restart), label = "dance",
    )

    // Моргание раз в несколько секунд, иногда двойное.
    val blink = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2200, 5200))
            repeat(if (Random.nextInt(4) == 0) 2 else 1) {
                blink.animateTo(1f, tween(70))
                blink.animateTo(0f, tween(110))
            }
        }
    }

    // Рот «говорит», пока звучит голос.
    val mouth = remember { Animatable(0f) }
    LaunchedEffect(talking) {
        if (!talking) {
            mouth.animateTo(0f, tween(120))
            return@LaunchedEffect
        }
        while (true) {
            mouth.animateTo(Random.nextFloat() * 0.8f + 0.2f, tween(90))
            mouth.animateTo(Random.nextFloat() * 0.25f, tween(90))
        }
    }

    // Случайные движения в покое и во время речи.
    var quirk by remember { mutableStateOf(Quirk.NONE) }
    LaunchedEffect(mood, talking) {
        quirk = Quirk.NONE
        val pool = when {
            talking || mood == NikaMood.TALK -> TALK_QUIRKS
            mood == NikaMood.IDLE -> IDLE_QUIRKS
            else -> return@LaunchedEffect
        }
        delay(Random.nextLong(800, 2000))
        while (true) {
            quirk = pool.random()
            delay(if (quirk == Quirk.WAVE || quirk == Quirk.HAIR) 2000 else 1300)
            quirk = Quirk.NONE
            delay(Random.nextLong(2200, 5000))
        }
    }

    // Подскоки (с приседом перед толчком и пружинистым приземлением) и подмигивание.
    val hop = remember { Animatable(0f) }
    val squat = remember { Animatable(0f) }
    val wink = remember { Animatable(0f) }
    LaunchedEffect(quirk) {
        when (quirk) {
            Quirk.HOP -> {
                squat.animateTo(1f, tween(170))
                coroutineScope {
                    launch { squat.animateTo(0f, tween(120)) }
                    hop.animateTo(-16f, tween(180, easing = FastOutSlowInEasing))
                }
                hop.animateTo(0f, tween(170, easing = FastOutLinearInEasing))
                squat.animateTo(0.7f, tween(80))
                squat.animateTo(0f, spring(dampingRatio = 0.45f))
            }
            Quirk.BOUNCE -> repeat(2) {
                squat.animateTo(0.5f, tween(110))
                coroutineScope {
                    launch { squat.animateTo(0f, tween(110)) }
                    hop.animateTo(-6f, tween(110))
                }
                hop.animateTo(0f, tween(120))
            }
            Quirk.WINK -> { wink.animateTo(1f, tween(90)); delay(380); wink.animateTo(0f, tween(140)) }
            else -> {}
        }
        // Если движение прервали на середине (например, Ника заговорила) — возвращаем всё на место.
        if (quirk != Quirk.WINK && wink.value != 0f) wink.animateTo(0f, tween(140))
        if (quirk != Quirk.HOP && quirk != Quirk.BOUNCE) {
            if (hop.value != 0f) hop.animateTo(0f, tween(160))
            if (squat.value != 0f) squat.animateTo(0f, tween(160))
        }
    }

    val wave = sin(fast * 2 * PI).toFloat()
    val dance = sin(beat * 2 * PI).toFloat()
    val gesture = sin(phase * 4 * PI).toFloat()

    // Позы рук: плечо, локоть, кисть. Для правой руки отрицательный угол — вверх и в сторону,
    // для левой — положительный (зеркально).
    val right = when {
        mood == NikaMood.HAPPY -> Pose(-115f - 8f * wave, -35f, -15f * wave)
        mood == NikaMood.DANCE -> Pose(-75f - 20f * dance, -80f + 25f * dance, -20f * dance)
        mood == NikaMood.WAVE || quirk == Quirk.WAVE -> Pose(-95f, -52f + 23f * wave, 15f * wave)
        mood == NikaMood.THINK -> Pose(-30f, -125f, -30f)
        mood == NikaMood.LISTEN -> Pose(-100f, -95f, -20f)
        mood == NikaMood.SURPRISED -> Pose(-40f, -75f, -10f)
        quirk == Quirk.SHRUG -> Pose(-35f, -80f, -30f)
        mood == NikaMood.SAD -> Pose(5f, 2f, 0f)
        talking -> Pose(-25f - 8f * gesture, -70f - 15f * gesture, -25f)
        else -> Pose(0f, 6f, 0f)
    }
    val left = when {
        mood == NikaMood.HAPPY -> Pose(115f + 8f * wave, 35f, 15f * wave)
        mood == NikaMood.DANCE -> Pose(40f - 25f * dance, 70f + 20f * dance, 10f)
        mood == NikaMood.SURPRISED -> Pose(40f, 75f, 10f)
        quirk == Quirk.SHRUG -> Pose(35f, 80f, 30f)
        mood == NikaMood.SAD -> Pose(-5f, -2f, 0f)
        talking -> Pose(8f, 15f + 6f * gesture, 0f)
        else -> Pose(0f, -6f, 0f)
    }
    val rs by animateFloatAsState(right.shoulder, spring(stiffness = 320f), label = "rs")
    val re by animateFloatAsState(right.elbow, spring(stiffness = 320f), label = "re")
    val rw by animateFloatAsState(right.wrist, spring(stiffness = 320f), label = "rw")
    val ls by animateFloatAsState(left.shoulder, spring(stiffness = 320f), label = "ls")
    val le by animateFloatAsState(left.elbow, spring(stiffness = 320f), label = "le")
    val lw by animateFloatAsState(left.wrist, spring(stiffness = 320f), label = "lw")

    val tilt by animateFloatAsState(
        when {
            mood == NikaMood.SAD -> 7f
            mood == NikaMood.LISTEN -> -8f
            mood == NikaMood.THINK -> 6f
            mood == NikaMood.DANCE -> 6f * dance
            quirk == Quirk.TILT -> 9f
            quirk == Quirk.LOOK_LEFT -> -4f
            quirk == Quirk.LOOK_RIGHT -> 4f
            quirk == Quirk.SHRUG -> -3f
            else -> 0f
        },
        spring(stiffness = 180f), label = "tilt",
    )
    val gazeX by animateFloatAsState(
        when {
            quirk == Quirk.LOOK_LEFT -> -4f
            quirk == Quirk.LOOK_RIGHT -> 4f
            mood == NikaMood.THINK -> -3f
            mood == NikaMood.LISTEN -> 3f
            else -> 0f
        },
        label = "gazeX",
    )
    val gazeY by animateFloatAsState(
        when {
            mood == NikaMood.SAD -> 3.5f
            mood == NikaMood.THINK || mood == NikaMood.LISTEN || quirk == Quirk.LOOK_UP -> -3.5f
            else -> 0f
        },
        label = "gazeY",
    )
    val eyeSize by animateFloatAsState(if (mood == NikaMood.SURPRISED) 1.15f else 1f, label = "eyeSize")
    val sadSquat by animateFloatAsState(if (mood == NikaMood.SAD) 0.25f else 0f, label = "sadSquat")

    val text = rememberTextMeasurer()
    val look = if (LocalNikaOutfit.current == NikaOutfit.STRICT) STRICT else CASUAL
    Canvas(modifier.aspectRatio(200f / 320f)) {
        val k = size.width / 200f
        val breath = sin(phase * 2 * PI).toFloat()
        // Прыжки: в «ура» Ника скачет, приседая у земли; в танце пружинит то на одной, то на другой ноге.
        val air = abs(sin(fast * PI).toFloat())
        val jump = when (mood) {
            NikaMood.HAPPY -> -air * 14f
            NikaMood.SURPRISED -> -4f
            else -> 0f
        } + hop.value
        val landing = if (mood == NikaMood.HAPPY) (1f - air).let { it * it * it } * 0.9f else 0f
        val kneeL = maxOf(squat.value, landing, sadSquat, if (mood == NikaMood.DANCE) maxOf(0f, dance) * 0.8f else 0f)
        val kneeR = maxOf(squat.value, landing, sadSquat, if (mood == NikaMood.DANCE) maxOf(0f, -dance) * 0.8f else 0f)
        // Насколько опускается тело, когда колени согнуты (бедро 30 + голень 34 единицы).
        val drop = 64f * (1f - cos(Math.toRadians(20.0 * maxOf(kneeL, kneeR))).toFloat())
        val bodyRot = when (mood) {
            NikaMood.DANCE -> dance * 6f
            NikaMood.IDLE, NikaMood.TALK -> breath * 1.2f
            else -> 0f
        }
        val dx = if (mood == NikaMood.DANCE) dance * 4f else 0f
        val sway = if (quirk == Quirk.HAIR || mood == NikaMood.DANCE) wave * 9f else breath * 2.5f
        val armsUp = abs(ls) > RAISED || abs(rs) > RAISED

        scale(k, k, pivot = Offset.Zero) {
            // Тень на полу: в прыжке становится меньше и бледнее.
            val lift = (-jump / 30f).coerceIn(0f, 0.6f)
            glow(Color.Black, Offset(100f + dx, 304f), 46f * (1f - lift * 0.5f), 7f * (1f - lift * 0.5f), 0.28f * (1f - lift))
            rotate(bodyRot, pivot = Offset(100f, 305f)) {
                translate(dx, jump) {
                    // Ноги: бедро вращается у пояса, голень — в колене (колени расходятся в стороны).
                    leg(look, left = true, bend = kneeL, drop = drop)
                    leg(look, left = false, bend = kneeR, drop = drop)

                    translate(0f, drop) {
                        // Волосы за головой и длинные пряди по бокам — пряди покачиваются.
                        drawPath(BACK_HAIR, Vol.backHair)
                        rotate(sway, pivot = Offset(46f, 118f)) {
                            drawPath(LOCK_L, Vol.lockL)
                            drawPath(GLOSS_L, Color(0x8C5A80FF), style = Stroke(3f, cap = StrokeCap.Round))
                            drawPath(GLOSS_L2, Color(0x808FA8FF), style = Stroke(2f, cap = StrokeCap.Round))
                            drawPath(GLOSS_L3, Color.White.copy(alpha = 0.45f), style = Stroke(1.2f, cap = StrokeCap.Round))
                        }
                        rotate(-sway, pivot = Offset(154f, 118f)) {
                            drawPath(LOCK_R, Vol.lockR)
                            drawPath(GLOSS_R, Color(0x595A80FF), style = Stroke(2.5f, cap = StrokeCap.Round))
                            drawPath(GLOSS_R2, Color(0x668FA8FF), style = Stroke(2f, cap = StrokeCap.Round))
                            drawPath(GLOSS_R3, Color.White.copy(alpha = 0.35f), style = Stroke(1.1f, cap = StrokeCap.Round))
                        }

                        // Опущенные руки — за туловищем, поднятые — поверх головы (иначе голова их закрывает).
                        if (abs(ls) <= RAISED) arm(look, left = true, ls + breath * 1.5f, le, lw)
                        if (abs(rs) <= RAISED) arm(look, left = false, rs - breath * 1.5f, re, rw)

                        drawRect(Vol.neck, Offset(95.5f, 170f), Size(9f, 18f))
                        translate(0f, breath * 0.8f) { torso(look, text) }

                        // Голова: наклон, лёгкое покачивание и кивки в такт речи.
                        translate(0f, breath * 1.2f + mouth.value * 1.5f) {
                            rotate(tilt + breath * 1.2f, pivot = Offset(100f, 172f)) {
                                // Объём лица: свет сверху-слева, блик на лбу, тень к подбородку.
                                drawCircle(Vol.face, 70f, Offset(100f, 105f))
                                glow(Color.White, Offset(88f, 82f), 26f, 14f, 0.55f)
                                drawCircle(Vol.chin, 70f, Offset(100f, 105f))
                                val blushA = if (mood == NikaMood.HAPPY || mood == NikaMood.DANCE || quirk == Quirk.WINK) 0.75f else 0.6f
                                glow(BLUSH, Offset(60f, 137f), 17f, 9f, blushA)
                                glow(BLUSH, Offset(140f, 137f), 17f, 9f, blushA)

                                if (mood == NikaMood.HAPPY || mood == NikaMood.DANCE) {
                                    happyEye(74f); happyEye(126f)
                                } else {
                                    eye(74f, -1f, maxOf(blink.value, wink.value), gazeX, gazeY, eyeSize)
                                    eye(126f, 1f, blink.value, gazeX, gazeY, eyeSize)
                                }
                                mouth(mood, mouth.value)
                                if (look.glasses) glasses()

                                // Чёлка с глянцевыми бликами.
                                drawPath(HAIR_CAP, Vol.cap)
                                drawPath(SHINE_1, Color(0xBF8FA8FF), style = Stroke(5f, cap = StrokeCap.Round))
                                drawPath(SHINE_2, Color(0x808FA8FF), style = Stroke(4f, cap = StrokeCap.Round))
                                drawPath(SHINE_3, Color.White.copy(alpha = 0.55f), style = Stroke(1.8f, cap = StrokeCap.Round))
                                drawPath(SHINE_4, Color.White.copy(alpha = 0.55f), style = Stroke(1.6f, cap = StrokeCap.Round))
                                drawPath(SHINE_5, Color(0x998FA8FF), style = Stroke(3f, cap = StrokeCap.Round))
                                drawPath(SHINE_6, Color(0xB3B5C5FF), style = Stroke(2.4f, cap = StrokeCap.Round))
                                for (sp in SPARKLES) drawPath(sp, Color.White.copy(alpha = 0.9f))
                                rotate(-18f, pivot = Offset(58f, 64f)) {
                                    scale(1.3f, pivot = Offset(58f, 64f)) {
                                        drawPath(BOW, look.bow)
                                        drawCircle(sphere(look.bowKnot, Offset(58f, 64f), 3.4f), 3.4f, Offset(58f, 64f))
                                    }
                                }

                                if (mood == NikaMood.THINK) thinkingDots(fast)
                                if (mood == NikaMood.DANCE) notes(beat)
                            }
                        }

                        if (abs(ls) > RAISED) arm(look, left = true, ls, le, lw)
                        if (abs(rs) > RAISED) arm(look, left = false, rs, re, rw)
                        // Верх костюма поверх поднятых рук: рукав выходит из-за плеча и не залезает на воротничок.
                        if (armsUp) translate(0f, breath * 0.8f) { torso(look, text) }
                    }
                }
            }
        }
    }
}

/** Поза руки: углы в плече, локте и запястье. */
private data class Pose(val shoulder: Float, val elbow: Float, val wrist: Float)

/** Светотень в духе 3D: градиенты вместо заливки и мягкие блики. Кисти считаются один раз. */
private fun shade(c: Color, f: Float): Color = if (f >= 0f) lerp(c, Color.White, f) else lerp(c, Color.Black, -f)

/** «Цилиндр»: края темнее, ближе к левому краю — полоса света. */
private fun cylinder(c: Color, b: Rect): Brush = Brush.horizontalGradient(
    0f to shade(c, -0.28f), 0.3f to shade(c, 0.16f), 0.6f to c, 1f to shade(c, -0.34f),
    startX = b.left, endX = b.right,
)

/** Сверху светлее, снизу темнее. */
private fun topLit(c: Color, b: Rect): Brush = Brush.verticalGradient(
    0f to shade(c, 0.35f), 0.5f to c, 1f to shade(c, -0.25f), startY = b.top, endY = b.bottom,
)

/** Шарик: блик сверху-слева. */
private fun sphere(c: Color, center: Offset, r: Float): Brush = Brush.radialGradient(
    0f to shade(c, 0.55f), 0.5f to c, 1f to shade(c, -0.35f),
    center = Offset(center.x - r * 0.3f, center.y - r * 0.4f), radius = r * 1.5f,
)

private fun ball(b: Rect, vararg stops: Pair<Float, Color>): Brush = Brush.radialGradient(
    *stops, center = Offset(b.left + b.width * 0.35f, b.top + b.height * 0.3f), radius = maxOf(b.width, b.height) * 0.8f,
)

/** Мягкое пятно (блик, тень, румянец) в форме эллипса. */
private fun DrawScope.glow(c: Color, center: Offset, rx: Float, ry: Float, alpha: Float) {
    scale(1f, ry / rx, pivot = center) {
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = alpha), c.copy(alpha = 0f)), center, rx), rx, center)
    }
}

/** Кисти, которые не зависят от наряда. */
private object Vol {
    private fun hair(p: Path) = p.getBounds().let { b ->
        Brush.linearGradient(
            0f to Color(0xFF2F55E6), 0.45f to HAIR, 1f to Color(0xFF0A176A),
            start = b.topLeft, end = Offset(b.left + b.width * 0.3f, b.bottom),
        )
    }
    val backHair = hair(BACK_HAIR)
    val lockL = hair(LOCK_L)
    val lockR = hair(LOCK_R)
    val cap = Brush.radialGradient(
        0f to Color(0xFF3B63F2), 0.55f to Color(0xFF1A38B8), 1f to Color(0xFF0C1C78),
        center = Offset(79f, 50f), radius = 110f,
    )
    val face = Brush.radialGradient(
        0f to Color(0xFFFFE9D4), 0.35f to Color(0xFFFCD3AE), 0.7f to Color(0xFFF2B98E),
        0.92f to Color(0xFFDE9A6E), 1f to Color(0xFFC9845B),
        center = Offset(86f, 80f), radius = 105f,
    )
    val chin = Brush.radialGradient(
        0f to Color.Transparent, 0.7f to Color.Transparent, 1f to Color(0x599C5E3E),
        center = Offset(100f, 94f), radius = 81f,
    )
    val neck = Brush.verticalGradient(
        0f to Color(0xFFB9785A), 0.5f to Color(0xFFE3A57E), 1f to Color(0xFFF0B990), startY = 170f, endY = 188f,
    )
    private fun hand(p: Path) = ball(p.getBounds(), 0f to Color(0xFFFFE1C6), 0.7f to Color(0xFFF2BC94), 1f to Color(0xFFD9976C))
    val handL = hand(HAND_L)
    val handR = hand(HAND_R)
    private fun shoe(x: Float) = Brush.radialGradient(
        0f to Color(0xFF5A5A66), 0.5f to SHOES, 1f to Color(0xFF0B0B0F), center = Offset(x - 3.5f, 299.5f), radius = 13f,
    )
    val shoeL = shoe(86f)
    val shoeR = shoe(114f)
    val collar = Brush.verticalGradient(
        0f to Color.White, 0.6f to Color(0xFFF2F4FA), 1f to Color(0xFFC9D0E2), startY = 182f, endY = 198f,
    )
    val star = ball(POCKET_STAR.getBounds(), 0f to Color(0xFFFFF3B0), 0.6f to STAR_GOLD, 1f to Color(0xFFE0A800))
    val upperLip = Brush.verticalGradient(listOf(Color(0xFFFF9EC2), Color(0xFFD2457C)), startY = 139f, endY = 148f)
}

/** Рука из двух частей: плечо вращается у плечевого сустава, предплечье — в локте, кисть — в запястье. */
private fun DrawScope.arm(look: Look, left: Boolean, shoulder: Float, elbow: Float, wrist: Float) {
    val s = if (left) Offset(84f, 190f) else Offset(116f, 190f)
    val e = if (left) Offset(76f, 221f) else Offset(124f, 221f)
    val w = if (left) Offset(68f, 251f) else Offset(132f, 251f)
    rotate(shoulder, pivot = s) {
        rotate(elbow, pivot = e) {
            rotate(wrist, pivot = w) { drawPath(if (left) HAND_L else HAND_R, if (left) Vol.handL else Vol.handR) }
            drawPath(if (left) FORE_L else FORE_R, if (left) look.foreL else look.foreR)
            drawPath(if (left) CUFF_L else CUFF_R, if (left) look.cuffL else look.cuffR)
        }
        drawPath(if (left) UPPER_L else UPPER_R, if (left) look.upperL else look.upperR)
        drawCircle(sphere(look.top, e, 5.6f), 5.6f, e) // локоть закрывает стык плеча и предплечья
    }
}

/** Нога из двух частей: бедро у пояса и голень в колене. bend 0..1 — насколько согнута. */
private fun DrawScope.leg(look: Look, left: Boolean, bend: Float, drop: Float) {
    val a = 20f * bend
    val hip = if (left) Offset(88f, 238f) else Offset(112f, 238f)
    val knee = if (left) Offset(89f, 266f) else Offset(111f, 266f)
    translate(0f, drop) {
        rotate(if (left) a else -a, pivot = hip) {
            rotate(if (left) -2 * a else 2 * a, pivot = knee) {
                drawOval(if (left) Vol.shoeL else Vol.shoeR, Offset(if (left) 73.5f else 101.5f, 296.5f), Size(25f, 11f))
                drawPath(if (left) SHIN_L else SHIN_R, if (left) look.shinL else look.shinR)
                drawPath(if (left) TRIM_L else TRIM_R, if (left) look.trimL else look.trimR)
                drawOval(Color.White.copy(alpha = 0.55f), Offset(if (left) 78f else 106f, 298.5f), Size(8f, 2.6f))
            }
            drawPath(if (left) THIGH_L else THIGH_R, if (left) look.thighL else look.thighR)
            if (left) drawPath(POCKET_STAR, Vol.star)
        }
    }
}

private fun DrawScope.torso(look: Look, text: TextMeasurer) {
    drawPath(TORSO, look.body)
    glow(Color(0xFF05183A), Offset(100f, 187f), 22f, 9f, 0.35f) // тень от головы на груди
    if (look.tie) {
        // Строгий пиджак: лацканы, галстук и две золотые пуговицы.
        drawPath(LAPELS, look.lapels)
        drawPath(TIE, look.tieBrush)
    }
    drawPath(COLLAR, Vol.collar)
    for (y in look.buttonsY) drawCircle(sphere(look.buttons, Offset(100f, y), 2f), 2f, Offset(100f, y))
    drawPath(BELT, look.beltBrush)
    drawPath(BUCKLE, look.buckleBrush)
    drawRoundRect(Color.White, Offset(103f, 195f), Size(20f, 9f), CornerRadius(1.5f))
    drawRoundRect(Color(0xFF333333), Offset(103f, 195f), Size(20f, 9f), CornerRadius(1.5f), style = Stroke(0.6f))
    val label = text.measure("НИКА", TextStyle(fontSize = 5.4f.toSp(), fontWeight = FontWeight.Bold, color = Color(0xFF222222)))
    drawText(label, topLeft = Offset(113f - label.size.width / 2f, 199.5f - label.size.height / 2f))
}

/** Глаз: белок, радужка с бликами и «стрелка» ресниц снаружи. side = -1 левый, 1 правый. */
private fun DrawScope.eye(cx: Float, side: Float, closed: Float, gazeX: Float, gazeY: Float, size: Float) {
    val open = 1f - closed * 0.92f
    scale(size, size * open, pivot = Offset(cx, 112f)) {
        val lash = Path().apply {
            moveTo(cx + side * 15f, 101f); lineTo(cx + side * 27f, 93f); lineTo(cx + side * 11f, 97f); close()
        }
        drawPath(lash, Color(0xFF111111))
        val white = Brush.radialGradient(
            0f to Color.White, 0.75f to Color(0xFFF3F5FB), 1f to Color(0xFFC8CEDD),
            center = Offset(cx - 1.7f, 106.6f), radius = 23f,
        )
        drawOval(white, Offset(cx - 17f, 94f), Size(34f, 36f))
        drawOval(Color(0xFF111111), Offset(cx - 17f, 94f), Size(34f, 36f), style = Stroke(2f))
        val ix = cx - side + gazeX // зрачки чуть ближе к носу
        val iy = 114f + gazeY
        val r = if (size > 1.05f) 10.5f else 12.5f
        val iris = Brush.radialGradient(
            0f to Color(0xFF4A4A55), 0.6f to Color(0xFF1E1E24), 1f to Color(0xFF050507),
            center = Offset(ix, iy + r * 0.3f), radius = r * 1.2f,
        )
        drawCircle(iris, r, Offset(ix, iy))
        // Отражение снизу, большой блик сверху и маленький — снизу.
        val rim = Path().apply { moveTo(ix - 8f, iy + 7f); quadraticBezierTo(ix - 1f, iy + 12.5f, ix + 6f, iy + 9f) }
        drawPath(rim, Color(0x737A7FA0), style = Stroke(1.6f, cap = StrokeCap.Round))
        drawOval(Color.White, Offset(ix - 9.1f, iy - 10.7f), Size(9.2f, 8.4f))
        drawCircle(Color.White.copy(alpha = 0.9f), 2f, Offset(ix + 5f, iy + 5.5f))
    }
}

/** Круглые очки отличницы. */
private fun DrawScope.glasses() {
    val frame = Color(0xFF2B2F45)
    for (cx in listOf(74f, 126f)) {
        drawCircle(Color.White.copy(alpha = 0.14f), 20.5f, Offset(cx, 112f))
        drawCircle(frame, 20.5f, Offset(cx, 112f), style = Stroke(2.4f))
    }
    val bridge = Path().apply { moveTo(94.5f, 109f); quadraticBezierTo(100f, 104f, 105.5f, 109f) }
    drawPath(bridge, frame, style = Stroke(2.2f, cap = StrokeCap.Round))
    drawLine(frame, Offset(53.6f, 108f), Offset(33f, 102f), strokeWidth = 2.2f, cap = StrokeCap.Round)
    drawLine(frame, Offset(146.4f, 108f), Offset(167f, 102f), strokeWidth = 2.2f, cap = StrokeCap.Round)
    // Блики на стёклах.
    for (cx in listOf(66f, 118f)) drawLine(Color.White.copy(alpha = 0.7f), Offset(cx, 100f), Offset(cx + 6f, 96f), strokeWidth = 2f, cap = StrokeCap.Round)
}

/** Счастливые глаза-дуги ^^ */
private fun DrawScope.happyEye(cx: Float) {
    val p = Path().apply { moveTo(cx - 13f, 117f); quadraticBezierTo(cx, 96f, cx + 13f, 117f) }
    drawPath(p, Color(0xFF111111), style = Stroke(3.2f, cap = StrokeCap.Round))
}

/** Верхняя губа «галочкой» с двумя горбиками; основание лежит на линии рта y. */
private fun upperLip(y: Float) = Path().apply {
    moveTo(92f, y); lineTo(95.4f, y - 5.2f); quadraticBezierTo(97.4f, y - 6.6f, 99f, y - 4.4f)
    lineTo(100f, y - 3.6f); lineTo(101f, y - 4.4f); quadraticBezierTo(102.6f, y - 6.6f, 104.6f, y - 5.2f)
    lineTo(108f, y); quadraticBezierTo(100f, y + 1.1f, 92f, y); close()
}

/** Нижняя губа — полукруг под линией рта; top — её верхний край. */
private fun DrawScope.lowerLip(top: Float, half: Float, depth: Float) {
    val p = Path().apply {
        moveTo(100f - half, top); quadraticBezierTo(100f, top + 1.6f, 100f + half, top)
        cubicTo(100f + half - 0.5f, top + depth * 0.7f, 103.5f, top + depth, 100f, top + depth)
        cubicTo(96.5f, top + depth, 100f - half + 0.5f, top + depth * 0.7f, 100f - half, top); close()
    }
    val lip = Brush.radialGradient(
        0f to Color(0xFFFFB8D3), 0.6f to Color(0xFFEC6A9E), 1f to Color(0xFFC23E74),
        center = Offset(100f - half * 0.2f, top + depth * 0.25f), radius = half * 1.5f,
    )
    drawPath(p, lip)
    drawOval(Color.White.copy(alpha = 0.72f), Offset(95.5f, top + depth * 0.36f), Size(4.2f, 1.9f))
}

/**
 * Рот по рисунку Вероники: закрытый — линия рта, верхняя губа «галочкой» и нижняя полукругом;
 * открытый — широкий рот, верхняя губа сидит на его верхнем краю, нижняя — под нижним.
 */
private fun DrawScope.mouth(mood: NikaMood, open: Float) {
    val line = Color(0xFF3B2F2A)
    val wide = when {
        mood == NikaMood.HAPPY || mood == NikaMood.DANCE -> maxOf(open, 0.55f)
        mood == NikaMood.SURPRISED -> maxOf(open, 0.3f)
        else -> open
    }
    when {
        mood == NikaMood.SAD && open < 0.05f -> {
            val p = Path().apply { moveTo(88f, 150f); cubicTo(94f, 143f, 106f, 143f, 112f, 150f) }
            drawPath(p, line, style = Stroke(1.4f, cap = StrokeCap.Round))
            drawPath(upperLip(146.6f), Vol.upperLip)
            lowerLip(146.9f, 7f, 6f)
        }
        wide > 0.05f -> {
            val bot = 150f + wide * 10f
            val m = Path().apply {
                moveTo(83f, 143.5f); quadraticBezierTo(100f, 147.5f, 117f, 143.5f)
                cubicTo(113f, bot - 5f, 106f, bot, 100f, bot)
                cubicTo(94f, bot, 87f, bot - 5f, 83f, 143.5f); close()
            }
            drawPath(m, Brush.verticalGradient(listOf(Color(0xFF3E0C1E), Color(0xFF7A2140)), startY = 143.5f, endY = bot))
            clipPath(m) {
                val tw = 7f + wide * 3f
                val th = 2.5f + wide * 2.5f
                drawOval(Color(0xFFE87A9A), Offset(100f - tw, bot + 0.5f - th), Size(tw * 2, th * 2))
            }
            val edge = Path().apply { moveTo(83f, 143.5f); quadraticBezierTo(100f, 147.5f, 117f, 143.5f) }
            drawPath(edge, line, style = Stroke(1.3f, cap = StrokeCap.Round))
            drawPath(upperLip(145.6f), Vol.upperLip)
            lowerLip(bot - 0.8f, 6.5f, 6.3f)
        }
        else -> {
            val p = Path().apply { moveTo(84f, 145f); quadraticBezierTo(100f, 148.6f, 116f, 144.5f) }
            drawPath(p, line, style = Stroke(1.3f, cap = StrokeCap.Round))
            drawPath(upperLip(146.6f), Vol.upperLip)
            lowerLip(146.9f, 7.5f, 7.7f)
            drawOval(Color.White.copy(alpha = 0.6f), Offset(95.3f, 141.9f), Size(2.4f, 1.4f))
        }
    }
}

private fun DrawScope.thinkingDots(t: Float) {
    for (i in 0..2) {
        val a = ((sin((t * 2 * PI) - i * 0.9) + 1) / 2).toFloat()
        drawCircle(Color(0xFF7E57C2).copy(alpha = 0.35f + 0.65f * a), 4f + i * 1.5f, Offset(165f + i * 11f, 30f - i * 10f))
    }
}

/** Ноты вокруг головы во время танца. */
private fun DrawScope.notes(t: Float) {
    val colors = listOf(Color(0xFFEC5F9C), Color(0xFF4A9FE8), Color(0xFFBA68C8))
    for (i in 0..2) {
        val p = (t + i / 3f) % 1f
        val x = listOf(20f, 178f, 40f)[i] + sin(p * 2 * PI).toFloat() * 6f
        val y = 70f - p * 50f
        val c = colors[i].copy(alpha = 1f - p)
        drawCircle(c, 4.5f, Offset(x, y))
        drawLine(c, Offset(x + 4f, y), Offset(x + 4f, y - 14f), strokeWidth = 1.8f)
        drawLine(c, Offset(x + 4f, y - 14f), Offset(x + 10f, y - 10f), strokeWidth = 1.8f)
    }
}

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

private val HAIR = Color(0xFF1631A8)
private val BLUSH = Color(0xFFF06292)
private val SUIT = Color(0xFF1E5FAF)
private val SUIT_TOP = Color(0xFF2368B5)
private val SHOES = Color(0xFF222222)
private val PINK = Color(0xFFEC5F9C)
private val PINK_LIGHT = Color(0xFFF48FB1)
private val STAR_GOLD = Color(0xFFFFD54F)

/** Цвета наряда и объёмные кисти для его деталей. */
private class Look(
    val suit: Color, val top: Color, val bodyColor: Color,
    val accent: Color, val bowKnot: Color,
    val cuff: Color, val trim: Color, val buttons: Color, val buttonsY: List<Float>,
    val belt: Color, val buckle: Color, val glasses: Boolean, val tie: Boolean,
) {
    val thighL by lazy { cylinder(suit, THIGH_L.getBounds()) }
    val thighR by lazy { cylinder(suit, THIGH_R.getBounds()) }
    val shinL by lazy { cylinder(suit, SHIN_L.getBounds()) }
    val shinR by lazy { cylinder(suit, SHIN_R.getBounds()) }
    val trimL by lazy { cylinder(trim, TRIM_L.getBounds()) }
    val trimR by lazy { cylinder(trim, TRIM_R.getBounds()) }
    val upperL by lazy { cylinder(top, UPPER_L.getBounds()) }
    val upperR by lazy { cylinder(top, UPPER_R.getBounds()) }
    val foreL by lazy { cylinder(top, FORE_L.getBounds()) }
    val foreR by lazy { cylinder(top, FORE_R.getBounds()) }
    val cuffL by lazy { cylinder(cuff, CUFF_L.getBounds()) }
    val cuffR by lazy { cylinder(cuff, CUFF_R.getBounds()) }
    val body by lazy { cylinder(bodyColor, TORSO.getBounds()) }
    val lapels by lazy { cylinder(top, LAPELS.getBounds()) }
    val tieBrush by lazy { topLit(accent, TIE.getBounds()) }
    val bow by lazy { topLit(accent, BOW.getBounds()) }
    val beltBrush by lazy { topLit(belt, BELT.getBounds()) }
    val buckleBrush by lazy { ball(BUCKLE.getBounds(), 0f to shade(buckle, 0.6f), 1f to buckle) }
}

private val CASUAL = Look(
    SUIT, SUIT_TOP, Color(0xFF2A72C8),
    PINK, PINK_LIGHT,
    PINK_LIGHT, PINK_LIGHT, PINK, listOf(206f, 215f, 224f),
    PINK, Color(0xFFF6B6CF), glasses = false, tie = false,
)

private val STRICT = Look(
    Color(0xFF263552), Color(0xFF2F4166), Color(0xFF34497A),
    Color(0xFFB23A48), Color(0xFFD9606E),
    Color(0xFFF4F6FA), Color(0xFF1B253D), Color(0xFFF2C14E), listOf(219f, 227f),
    Color(0xFF1B253D), Color(0xFFF2C14E), glasses = true, tie = true,
)

private val TIE = svg("M98,194.5 L102,194.5 L103.6,199 L101.2,213.5 L100,215.5 L98.8,213.5 L96.4,199 Z")
private val LAPELS = svg("M84,185 L98,196 L92,214 L80,198 Z M116,185 L102,196 L108,214 L120,198 Z")

private val LOCK_L = svg("M40,112 C26,130 20,160 22,190 C23,215 16,235 9,251 Q18,257 26,250 Q33,261 42,252 Q52,259 58,246 C54,230 58,205 60,180 C62,160 64,140 68,128 Z")
private val LOCK_R = svg("M160,112 C174,130 180,160 178,190 C177,215 184,235 191,251 Q182,257 174,250 Q167,261 158,252 Q148,259 142,246 C146,230 142,205 140,180 C138,160 136,140 132,128 Z")
private val BACK_HAIR = svg("M30,108 C30,60 60,32 100,32 C140,32 170,60 170,108 L170,142 L30,142 Z")
private val UPPER_L = svg("M89,192 C88,186 80,184 78,189 L70,219 C72,225 80,226 82,221 Z")
private val UPPER_R = svg("M111,192 C112,186 120,184 122,189 L130,219 C128,225 120,226 118,221 Z")
private val FORE_L = svg("M82,219 L70,219 C68,232 63,242 56,251 L80,251 C80,241 81,231 82,219 Z")
private val FORE_R = svg("M118,219 L130,219 C132,232 137,242 144,251 L120,251 C120,241 119,231 118,219 Z")
private val CUFF_L = svg("M57.5,248 L80.2,247 L80,251 L56,252 Z")
private val CUFF_R = svg("M142.5,248 L119.8,247 L120,251 L144,252 Z")
private val HAND_L = svg("M71.5,250 C73.5,256 71,261.5 67,261.5 C63,261.5 61.5,256 64,250 Z")
private val HAND_R = svg("M128.5,250 C126.5,256 129,261.5 133,261.5 C137,261.5 138.5,256 136,250 Z")
private val THIGH_L = svg("M77,236 L99,236 L98.5,268 L79.5,268 Z")
private val THIGH_R = svg("M123,236 L101,236 L101.5,268 L120.5,268 Z")
private val SHIN_L = svg("M79.5,264 L98.5,264 C98,282 99,292 100,300 L72,300 C75,290 79,280 79.5,264 Z")
private val SHIN_R = svg("M120.5,264 L101.5,264 C102,282 101,292 100,300 L128,300 C125,290 121,280 120.5,264 Z")
private val TRIM_L = svg("M73.6,294 L99.6,294 L100,300 L72,300 Z")
private val TRIM_R = svg("M126.4,294 L100.4,294 L100,300 L128,300 Z")
private val POCKET_STAR = svg("M88.0,247.8 L89.1,250.5 L92.0,250.7 L89.7,252.6 L90.5,255.4 L88.0,253.8 L85.5,255.4 L86.3,252.6 L84.0,250.7 L86.9,250.5 Z")
private val TORSO = svg("M84,184 L116,184 C121,186 122,192 122,200 L124,240 L76,240 L78,200 C78,192 79,186 84,184 Z")
private val HAIR_CAP = svg("M22,120 C20,70 55,30 100,28 C150,26 184,60 186,124 C178,112 168,98 160,86 C148,70 132,58 116,48 C108,62 92,72 72,84 C55,94 38,106 22,120 Z")
private val COLLAR = svg("M100,186 C94,183 87,182 83,185 C83,192 90,198 98,195 Z M100,186 C106,183 113,182 117,185 C117,192 110,198 102,195 Z")
private val BELT = svg("M76.6,231 L123.4,231 L123.7,237 L76.3,237 Z")
private val BUCKLE = svg("M100,238 C96,235.5 95,232.5 97.5,231.8 C99,231.4 100,232.6 100,233.2 C100,232.6 101,231.4 102.5,231.8 C105,232.5 104,235.5 100,238 Z")
private val BOW = svg("M58,64 L47,57 L46,71 Z M58,64 L69,57 L70,71 Z")
private val SHINE_1 = svg("M46,72 C60,52 78,42 96,38")
private val SHINE_2 = svg("M130,42 C148,48 162,60 170,78")
private val SHINE_3 = svg("M60,58 C72,47 84,42 92,40")
private val SHINE_4 = svg("M140,46 C152,52 160,61 166,71")
private val SHINE_5 = svg("M34,96 C40,86 46,79 52,74")
private val SHINE_6 = svg("M104,34 C112,33 120,34 126,36")
private val GLOSS_L2 = svg("M53,150 C49,180 48,210 46,242")
private val GLOSS_L3 = svg("M43,150 C38,176 37,200 33,226")
private val GLOSS_R2 = svg("M147,150 C151,180 152,210 154,242")
private val GLOSS_R3 = svg("M158,152 C163,178 164,200 168,226")

/** Искорки-звёздочки на волосах. */
private val SPARKLES = listOf(Triple(98f, 40f, 1.6f), Triple(84f, 44f, 1f), Triple(158f, 60f, 1.2f)).map { (x, y, r) ->
    val d = r * 2.4f
    Path().apply {
        moveTo(x, y - d); quadraticBezierTo(x, y, x + d, y); quadraticBezierTo(x, y, x, y + d)
        quadraticBezierTo(x, y, x - d, y); quadraticBezierTo(x, y, x, y - d); close()
    }
}
private val GLOSS_L = svg("M46,140 C40,170 38,205 32,238")
private val GLOSS_R = svg("M156,140 C163,172 165,205 170,236")
