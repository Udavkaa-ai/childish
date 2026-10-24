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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** Настроение Ники — от него зависит анимация. */
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
fun Nika(mood: NikaMood, modifier: Modifier = Modifier, talking: Boolean = mood == NikaMood.TALK) {
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

    // Подскоки и подмигивание — короткие одноразовые анимации.
    val hop = remember { Animatable(0f) }
    val wink = remember { Animatable(0f) }
    LaunchedEffect(quirk) {
        when (quirk) {
            Quirk.HOP -> { hop.animateTo(-14f, tween(170)); hop.animateTo(0f, spring(dampingRatio = 0.4f)) }
            Quirk.BOUNCE -> repeat(2) { hop.animateTo(-6f, tween(120)); hop.animateTo(0f, tween(140)) }
            Quirk.WINK -> { wink.animateTo(1f, tween(90)); delay(380); wink.animateTo(0f, tween(140)) }
            else -> {}
        }
    }

    val wave = sin(fast * 2 * PI).toFloat()
    val dance = sin(beat * 2 * PI).toFloat()
    val gesture = sin(phase * 4 * PI).toFloat()
    val armLTarget = when {
        mood == NikaMood.HAPPY -> 135f + 15f * wave
        mood == NikaMood.DANCE -> 95f + 55f * dance
        mood == NikaMood.SURPRISED || quirk == Quirk.SHRUG -> 38f
        mood == NikaMood.SAD -> -6f
        talking -> 8f + 6f * gesture
        else -> 0f
    }
    val armRTarget = when {
        mood == NikaMood.HAPPY -> -135f - 15f * wave
        mood == NikaMood.DANCE -> -95f + 55f * dance
        mood == NikaMood.WAVE || quirk == Quirk.WAVE -> -125f + 20f * wave
        mood == NikaMood.THINK -> -165f
        mood == NikaMood.LISTEN -> -140f
        mood == NikaMood.SURPRISED || quirk == Quirk.SHRUG -> -38f
        mood == NikaMood.SAD -> 6f
        talking -> -28f - 18f * gesture
        else -> 0f
    }
    val armL by animateFloatAsState(armLTarget, spring(stiffness = 320f), label = "armL")
    val armR by animateFloatAsState(armRTarget, spring(stiffness = 320f), label = "armR")
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

    val text = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(200f / 320f)) {
        val k = size.width / 200f
        val breath = sin(phase * 2 * PI).toFloat()
        val jump = when (mood) {
            NikaMood.HAPPY -> -abs(sin(fast * PI).toFloat()) * 14f
            NikaMood.DANCE -> -abs(dance) * 6f
            NikaMood.SURPRISED -> -4f
            NikaMood.SAD -> 3f
            else -> 0f
        } + hop.value
        val bodyRot = when (mood) {
            NikaMood.DANCE -> dance * 7f
            NikaMood.IDLE, NikaMood.TALK -> breath * 1.2f
            else -> 0f
        }
        val dx = if (mood == NikaMood.DANCE) dance * 5f else 0f
        val sway = if (quirk == Quirk.HAIR || mood == NikaMood.DANCE) wave * 9f else breath * 2.5f

        scale(k, k, pivot = Offset.Zero) {
            rotate(bodyRot, pivot = Offset(100f, 305f)) {
                translate(dx, jump) {
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
                    if (abs(armL) <= RAISED) armLeft(armL)
                    if (abs(armR) <= RAISED) armRight(armR)

                    // Туловище: туфли, брюки-клёш, шея, комбинезон, бейдж.
                    drawOval(SHOES, Offset(74f, 297f), Size(24f, 10f))
                    drawOval(SHOES, Offset(102f, 297f), Size(24f, 10f))
                    fillStroke(LEG_L, SUIT, SUIT_LINE)
                    fillStroke(LEG_R, SUIT, SUIT_LINE)
                    drawRect(SKIN, Offset(96f, 170f), Size(8f, 18f))
                    translate(0f, breath * 0.8f) { torso(text) }

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

                            fillStroke(HAIR_CAP, HAIR, HAIR_LINE)
                            drawPath(SHINE_1, SHINE, style = Stroke(3f, cap = StrokeCap.Round))
                            drawPath(SHINE_2, SHINE, style = Stroke(3f, cap = StrokeCap.Round))

                            if (mood == NikaMood.THINK) thinkingDots(fast)
                            if (mood == NikaMood.DANCE) notes(beat)
                        }
                    }

                    if (abs(armL) > RAISED) armLeft(armL)
                    if (abs(armR) > RAISED) armRight(armR)
                }
            }
        }
    }
}

private val thin = Stroke(1.2f, join = StrokeJoin.Round)

private fun DrawScope.fillStroke(path: Path, fill: Color, line: Color) {
    drawPath(path, fill)
    drawPath(path, line, style = thin)
}

/** Руки вращаются вокруг плеча; кисть — на конце рукава. */
private fun DrawScope.armLeft(angle: Float) = rotate(angle, pivot = Offset(84f, 190f)) {
    drawCircle(SKIN, 5f, Offset(68f, 254f)); drawCircle(SUIT_LINE, 5f, Offset(68f, 254f), style = thin)
    fillStroke(ARM_L, SUIT_TOP, SUIT_LINE)
}

private fun DrawScope.armRight(angle: Float) = rotate(angle, pivot = Offset(116f, 190f)) {
    drawCircle(SKIN, 5f, Offset(132f, 254f)); drawCircle(SUIT_LINE, 5f, Offset(132f, 254f), style = thin)
    fillStroke(ARM_R, SUIT_TOP, SUIT_LINE)
}

private fun DrawScope.torso(text: TextMeasurer) {
    fillStroke(TORSO, SUIT_TOP, SUIT_LINE)
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

private val LOCK_L = svg("M40,112 C26,130 20,160 22,190 C23,215 16,235 9,251 Q18,257 26,250 Q33,261 42,252 Q52,259 58,246 C54,230 58,205 60,180 C62,160 64,140 68,128 Z")
private val LOCK_R = svg("M160,112 C174,130 180,160 178,190 C177,215 184,235 191,251 Q182,257 174,250 Q167,261 158,252 Q148,259 142,246 C146,230 142,205 140,180 C138,160 136,140 132,128 Z")
private val STRANDS_L = svg("M42,128 C32,160 30,200 22,240 M52,140 C46,175 44,215 40,248")
private val STRANDS_R = svg("M158,128 C168,160 170,200 178,240 M148,140 C154,175 156,215 160,248")
private val BACK_HAIR = svg("M30,108 C30,60 60,32 100,32 C140,32 170,60 170,108 L170,142 L30,142 Z")
private val ARM_L = svg("M82,186 C74,199 69,220 67,236 L56,252 L80,251 L80,238 C82,222 85,206 88,193 Z")
private val ARM_R = svg("M118,186 C126,199 131,220 133,236 L144,252 L120,251 L120,238 C118,222 115,206 112,193 Z")
private val LEG_L = svg("M77,236 L99,236 L98,266 C98,282 99,292 100,300 L72,300 C75,290 79,280 80,266 Z")
private val LEG_R = svg("M123,236 L101,236 L102,266 C102,282 101,292 100,300 L128,300 C125,290 121,280 120,266 Z")
private val TORSO = svg("M84,184 L116,184 C121,186 122,192 122,200 L124,240 L76,240 L78,200 C78,192 79,186 84,184 Z")
private val HAIR_CAP = svg("M22,120 C20,70 55,30 100,28 C150,26 184,60 186,124 C178,112 168,98 160,86 C148,70 132,58 116,48 C108,62 92,72 72,84 C55,94 38,106 22,120 Z")
private val SHINE_1 = svg("M48,70 C60,52 78,42 96,38")
private val SHINE_2 = svg("M128,40 C148,46 164,60 172,80")
private val SMILE = svg("M86,144 C92,151 108,151 116,142")
private val SMILE_BIG = svg("M84,141 C92,156 110,156 118,139")
private val LIPS_PATH = svg("M93,147 C93,143 97,142 99,144 C101,142 105,143 105,147 C105,151 100,153 99,152 C97,153 93,151 93,147 Z")
