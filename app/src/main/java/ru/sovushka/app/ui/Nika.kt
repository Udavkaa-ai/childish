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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
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
            rotate(bodyRot, pivot = Offset(100f, 305f)) {
                translate(dx, jump) {
                    // Ноги: бедро вращается у пояса, голень — в колене (колени расходятся в стороны).
                    leg(look, left = true, bend = kneeL, drop = drop)
                    leg(look, left = false, bend = kneeR, drop = drop)

                    translate(0f, drop) {
                        // Волосы за головой и длинные пряди по бокам — пряди покачиваются.
                        fillStroke(BACK_HAIR, HAIR, HAIR_LINE)
                        rotate(sway, pivot = Offset(46f, 118f)) {
                            fillStroke(LOCK_L, HAIR, HAIR_LINE)
                            drawPath(STRANDS_L, STRAND, style = Stroke(1f, cap = StrokeCap.Round))
                        }
                        rotate(-sway, pivot = Offset(154f, 118f)) {
                            fillStroke(LOCK_R, HAIR, HAIR_LINE)
                            drawPath(STRANDS_R, STRAND, style = Stroke(1f, cap = StrokeCap.Round))
                        }

                        // Опущенные руки — за туловищем, поднятые — поверх головы (иначе голова их закрывает).
                        if (abs(ls) <= RAISED) arm(look, left = true, ls + breath * 1.5f, le, lw)
                        if (abs(rs) <= RAISED) arm(look, left = false, rs - breath * 1.5f, re, rw)

                        drawRect(SKIN, Offset(96f, 170f), Size(8f, 18f))
                        translate(0f, breath * 0.8f) { torso(look, text) }

                        // Голова: наклон, лёгкое покачивание и кивки в такт речи.
                        translate(0f, breath * 1.2f + mouth.value * 1.5f) {
                            rotate(tilt + breath * 1.2f, pivot = Offset(100f, 172f)) {
                                drawCircle(SKIN, 70f, Offset(100f, 105f))
                                drawCircle(OUTLINE, 70f, Offset(100f, 105f), style = thin)
                                val blushA = if (mood == NikaMood.HAPPY || mood == NikaMood.DANCE || quirk == Quirk.WINK) 0.65f else 0.45f
                                drawOval(BLUSH.copy(alpha = blushA), Offset(49f, 132.5f), Size(24f, 9f))
                                drawOval(BLUSH.copy(alpha = blushA), Offset(127f, 132.5f), Size(24f, 9f))

                                if (mood == NikaMood.HAPPY || mood == NikaMood.DANCE) {
                                    happyEye(74f); happyEye(126f)
                                } else {
                                    eye(74f, -1f, maxOf(blink.value, wink.value), gazeX, gazeY, eyeSize)
                                    eye(126f, 1f, blink.value, gazeX, gazeY, eyeSize)
                                }
                                mouth(mood, mouth.value)
                                if (look.glasses) glasses()

                                fillStroke(HAIR_CAP, HAIR, HAIR_LINE)
                                drawPath(SHINE_1, SHINE, style = Stroke(3f, cap = StrokeCap.Round))
                                drawPath(SHINE_2, SHINE, style = Stroke(3f, cap = StrokeCap.Round))
                                rotate(-18f, pivot = Offset(58f, 64f)) {
                                    scale(1.3f, pivot = Offset(58f, 64f)) {
                                        drawPath(BOW, look.accent)
                                        drawPath(BOW, look.accentDark, style = Stroke(0.9f, join = StrokeJoin.Round))
                                        drawCircle(look.bowKnot, 3.4f, Offset(58f, 64f))
                                        drawCircle(look.accentDark, 3.4f, Offset(58f, 64f), style = Stroke(0.9f))
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

private val thin = Stroke(1.2f, join = StrokeJoin.Round)

private fun DrawScope.fillStroke(path: Path, fill: Color, line: Color) {
    drawPath(path, fill)
    drawPath(path, line, style = thin)
}

/** Рука из двух частей: плечо вращается у плечевого сустава, предплечье — в локте, кисть — в запястье. */
private fun DrawScope.arm(look: Look, left: Boolean, shoulder: Float, elbow: Float, wrist: Float) {
    val s = if (left) Offset(84f, 190f) else Offset(116f, 190f)
    val e = if (left) Offset(76f, 221f) else Offset(124f, 221f)
    val w = if (left) Offset(68f, 251f) else Offset(132f, 251f)
    rotate(shoulder, pivot = s) {
        rotate(elbow, pivot = e) {
            rotate(wrist, pivot = w) { fillStroke(if (left) HAND_L else HAND_R, SKIN, look.line) }
            fillStroke(if (left) FORE_L else FORE_R, look.top, look.line)
            trim(look, if (left) CUFF_L else CUFF_R, look.cuff)
        }
        fillStroke(if (left) UPPER_L else UPPER_R, look.top, look.line)
        drawCircle(look.top, 5.6f, e) // локоть закрывает стык плеча и предплечья
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
                drawOval(SHOES, Offset(if (left) 74f else 102f, 297f), Size(24f, 10f))
                fillStroke(if (left) SHIN_L else SHIN_R, look.suit, look.line)
                trim(look, if (left) TRIM_L else TRIM_R, look.trim)
                drawOval(Color.White.copy(alpha = 0.6f), Offset(if (left) 77.5f else 105.5f, 298.7f), Size(7f, 2.6f))
            }
            fillStroke(if (left) THIGH_L else THIGH_R, look.suit, look.line)
            if (left) {
                drawPath(POCKET_STAR, STAR_GOLD)
                drawPath(POCKET_STAR, Color(0xFFC58A00), style = Stroke(0.5f))
            }
        }
    }
}

/** Отделка: манжеты и полоски внизу брюк. */
private fun DrawScope.trim(look: Look, path: Path, color: Color) {
    drawPath(path, color)
    drawPath(path, look.line, style = Stroke(0.8f))
}

private fun DrawScope.torso(look: Look, text: TextMeasurer) {
    drawPath(TORSO, look.gradient)
    drawPath(TORSO, look.line, style = thin)
    if (look.tie) {
        // Строгий пиджак: лацканы, галстук и две золотые пуговицы.
        drawPath(LAPELS, look.top)
        drawPath(LAPELS, look.line, style = Stroke(0.8f))
        drawPath(TIE, look.accent)
        drawPath(TIE, look.accentDark, style = Stroke(0.7f))
    }
    drawPath(COLLAR, Color.White)
    drawPath(COLLAR, Color(0xFF9AA7C4), style = Stroke(0.8f))
    for (y in look.buttonsY) {
        drawCircle(look.buttons, 1.9f, Offset(100f, y))
        if (look.tie) drawCircle(look.accentDark, 1.9f, Offset(100f, y), style = Stroke(0.5f))
    }
    drawPath(BELT, look.belt)
    drawPath(BELT, look.accentDark, style = Stroke(0.8f))
    drawPath(BUCKLE, look.buckle)
    drawPath(BUCKLE, look.accentDark, style = Stroke(0.7f))
    drawRoundRect(Color.White, Offset(103f, 195f), Size(20f, 9f), CornerRadius(1.5f))
    drawRoundRect(Color(0xFF333333), Offset(103f, 195f), Size(20f, 9f), CornerRadius(1.5f), style = Stroke(0.7f))
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
        drawOval(Color.White, Offset(cx - 17f, 94f), Size(34f, 36f))
        drawOval(Color(0xFF111111), Offset(cx - 17f, 94f), Size(34f, 36f), style = Stroke(1.6f))
        val ix = cx - side + gazeX // зрачки чуть ближе к носу
        val iy = 114f + gazeY
        val r = if (size > 1.05f) 10.5f else 12.5f
        drawCircle(Color(0xFF2B2B2B), r, Offset(ix, iy))
        drawCircle(Color.White, 4f, Offset(ix - 5f, iy - 6f))
        drawCircle(Color.White, 2f, Offset(ix + 5f, iy + 5f))
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

private fun DrawScope.mouth(mood: NikaMood, open: Float) {
    val line = Color(0xFF3B2F2A)
    when {
        mood == NikaMood.SAD -> {
            val p = Path().apply { moveTo(90f, 150f); cubicTo(95f, 143f, 105f, 143f, 110f, 150f) }
            drawPath(p, line, style = Stroke(1.6f, cap = StrokeCap.Round))
        }
        mood == NikaMood.SURPRISED && open < 0.05f -> {
            drawOval(Color(0xFF8E2547), Offset(95f, 141f), Size(10f, 12f))
        }
        open > 0.05f -> {
            val h = 3f + open * 10f
            drawOval(Color(0xFF8E2547), Offset(92f, 146f - h / 2f), Size(16f, h))
            drawOval(LIPS, Offset(92f, 146f - h / 2f - 1.5f), Size(16f, 3.5f))
        }
        else -> {
            val smile = if (mood == NikaMood.HAPPY || mood == NikaMood.DANCE) SMILE_BIG else SMILE
            drawPath(smile, line, style = Stroke(1.3f, cap = StrokeCap.Round))
            drawPath(LIPS_PATH, LIPS)
            drawPath(LIPS_PATH, Color(0xFF9C2F5E), style = Stroke(0.8f))
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
private val HAIR_LINE = Color(0xFF0E1F6E)
private val STRAND = Color(0x990E1F6E)
private val SHINE = Color(0xB33D63E0)
private val SKIN = Color(0xFFF7C8A0)
private val OUTLINE = Color(0xFF3B2F2A)
private val BLUSH = Color(0xFFF06292)
private val SUIT = Color(0xFF1E5FAF)
private val SUIT_TOP = Color(0xFF2368B5)
private val SUIT_LINE = Color(0xFF173F75)
private val SHOES = Color(0xFF222222)
private val LIPS = Color(0xFFE86A9A)
private val PINK = Color(0xFFEC5F9C)
private val PINK_LIGHT = Color(0xFFF48FB1)
private val PINK_DARK = Color(0xFFA8326A)
private val STAR_GOLD = Color(0xFFFFD54F)
private val SUIT_GRADIENT = Brush.verticalGradient(listOf(Color(0xFF3584D6), Color(0xFF2160AE)), startY = 184f, endY = 240f)

/** Цвета наряда. */
private class Look(
    val suit: Color, val top: Color, val line: Color, val gradient: Brush,
    val accent: Color, val accentDark: Color, val bowKnot: Color,
    val cuff: Color, val trim: Color, val buttons: Color, val buttonsY: List<Float>,
    val belt: Color, val buckle: Color, val glasses: Boolean, val tie: Boolean,
)

private val CASUAL = Look(
    SUIT, SUIT_TOP, SUIT_LINE, SUIT_GRADIENT,
    PINK, PINK_DARK, PINK_LIGHT,
    PINK_LIGHT, PINK_LIGHT, PINK, listOf(206f, 215f, 224f),
    PINK, Color(0xFFFFE3EE), glasses = false, tie = false,
)

private val STRICT = Look(
    Color(0xFF263552), Color(0xFF2F4166), Color(0xFF141C30),
    Brush.verticalGradient(listOf(Color(0xFF3C5280), Color(0xFF26365A)), startY = 184f, endY = 240f),
    Color(0xFFB23A48), Color(0xFF6E1F2A), Color(0xFFD9606E),
    Color(0xFFF4F6FA), Color(0xFF1B253D), Color(0xFFF2C14E), listOf(219f, 227f),
    Color(0xFF1B253D), Color(0xFFF2C14E), glasses = true, tie = true,
)

private val TIE = svg("M98,194.5 L102,194.5 L103.6,199 L101.2,213.5 L100,215.5 L98.8,213.5 L96.4,199 Z")
private val LAPELS = svg("M84,185 L98,196 L92,214 L80,198 Z M116,185 L102,196 L108,214 L120,198 Z")

private val LOCK_L = svg("M40,112 C26,130 20,160 22,190 C23,215 16,235 9,251 Q18,257 26,250 Q33,261 42,252 Q52,259 58,246 C54,230 58,205 60,180 C62,160 64,140 68,128 Z")
private val LOCK_R = svg("M160,112 C174,130 180,160 178,190 C177,215 184,235 191,251 Q182,257 174,250 Q167,261 158,252 Q148,259 142,246 C146,230 142,205 140,180 C138,160 136,140 132,128 Z")
private val STRANDS_L = svg("M42,128 C32,160 30,200 22,240 M52,140 C46,175 44,215 40,248")
private val STRANDS_R = svg("M158,128 C168,160 170,200 178,240 M148,140 C154,175 156,215 160,248")
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
private val SHINE_1 = svg("M48,70 C60,52 78,42 96,38")
private val SHINE_2 = svg("M128,40 C148,46 164,60 172,80")
private val SMILE = svg("M86,144 C92,151 108,151 116,142")
private val SMILE_BIG = svg("M84,141 C92,156 110,156 118,139")
private val LIPS_PATH = svg("M93,147 C93,143 97,142 99,144 C101,142 105,143 105,147 C105,151 100,153 99,152 C97,153 93,151 93,147 Z")
