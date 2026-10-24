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
import androidx.compose.runtime.remember
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
enum class NikaMood { IDLE, TALK, HAPPY, SAD, LISTEN, THINK, WAVE }

/**
 * Ника — персонаж, которого придумала Вероника. Нарисована векторно по её рисунку
 * (исходник — art/nika.svg, координаты в той же системе 200×320) и разложена на части,
 * чтобы двигать голову, глаза, рот, руки и волосы.
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

    // Моргание раз в несколько секунд.
    val blink = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2200, 5200))
            blink.animateTo(1f, tween(70))
            blink.animateTo(0f, tween(110))
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

    val armLTarget = when (mood) {
        NikaMood.HAPPY -> 135f + 15f * sin(fast * 2 * PI).toFloat()
        NikaMood.THINK -> 20f
        else -> 0f
    }
    val armRTarget = when (mood) {
        NikaMood.HAPPY -> -135f - 15f * sin(fast * 2 * PI).toFloat()
        NikaMood.WAVE -> -140f + 25f * sin(fast * 2 * PI).toFloat()
        NikaMood.THINK -> -115f
        NikaMood.LISTEN -> -150f
        else -> 0f
    }
    val armL by animateFloatAsState(armLTarget, spring(stiffness = 300f), label = "armL")
    val armR by animateFloatAsState(armRTarget, spring(stiffness = 300f), label = "armR")
    val tilt by animateFloatAsState(
        when (mood) {
            NikaMood.SAD -> 7f
            NikaMood.LISTEN -> -7f
            NikaMood.THINK -> 5f
            else -> 0f
        },
        spring(stiffness = 200f), label = "tilt",
    )
    val look by animateFloatAsState(
        when (mood) {
            NikaMood.SAD -> 1f
            NikaMood.THINK, NikaMood.LISTEN -> -1f
            else -> 0f
        },
        label = "look",
    )

    val text = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(200f / 320f)) {
        val k = size.width / 200f
        val breath = sin(phase * 2 * PI).toFloat()
        val jump = if (mood == NikaMood.HAPPY) -abs(sin(fast * PI).toFloat()) * 14f else 0f
        val sway = breath * 2.5f

        scale(k, k, pivot = Offset.Zero) {
            translate(0f, jump + if (mood == NikaMood.SAD) 3f else 0f) {
                // Пряди сбоку (за головой) — покачиваются.
                rotate(sway, pivot = Offset(46f, 118f)) { fillStroke(LOCK_L, HAIR, HAIR_LINE) }
                rotate(-sway, pivot = Offset(154f, 118f)) { fillStroke(LOCK_R, HAIR, HAIR_LINE) }

                // Руки (за туловищем), вращаются вокруг плеча.
                rotate(armL, pivot = Offset(84f, 190f)) {
                    drawCircle(SKIN, 5f, Offset(68f, 254f)); drawCircle(SUIT_LINE, 5f, Offset(68f, 254f), style = thin)
                    fillStroke(ARM_L, SUIT_TOP, SUIT_LINE)
                }
                rotate(armR, pivot = Offset(116f, 190f)) {
                    drawCircle(SKIN, 5f, Offset(132f, 254f)); drawCircle(SUIT_LINE, 5f, Offset(132f, 254f), style = thin)
                    fillStroke(ARM_R, SUIT_TOP, SUIT_LINE)
                }

                // Туловище: туфли, брюки-клёш, шея, комбинезон, бейдж.
                drawOval(SHOES, Offset(74f, 297f), Size(24f, 10f))
                drawOval(SHOES, Offset(102f, 297f), Size(24f, 10f))
                fillStroke(LEG_L, SUIT, SUIT_LINE)
                fillStroke(LEG_R, SUIT, SUIT_LINE)
                drawRect(SKIN, Offset(96f, 170f), Size(8f, 18f))
                translate(0f, breath * 0.8f) {
                    fillStroke(TORSO, SUIT_TOP, SUIT_LINE)
                    drawRoundRect(Color.White, Offset(103f, 195f), Size(20f, 9f), CornerRadius(1.5f))
                    drawRoundRect(Color(0xFF333333), Offset(103f, 195f), Size(20f, 9f), CornerRadius(1.5f), style = Stroke(0.7f))
                    val label = text.measure("НИКА", TextStyle(fontSize = 5.4f.toSp(), fontWeight = FontWeight.Bold, color = Color(0xFF222222)))
                    drawText(label, topLeft = Offset(113f - label.size.width / 2f, 199.5f - label.size.height / 2f))
                }

                // Голова целиком: наклон и лёгкое покачивание.
                translate(0f, breath * 1.2f) {
                    rotate(tilt + breath * 1.2f, pivot = Offset(100f, 172f)) {
                        drawCircle(SKIN, 70f, Offset(100f, 105f))
                        drawCircle(OUTLINE, 70f, Offset(100f, 105f), style = thin)
                        val blushA = if (mood == NikaMood.HAPPY) 0.65f else 0.45f
                        drawOval(BLUSH.copy(alpha = blushA), Offset(49f, 132.5f), Size(24f, 9f))
                        drawOval(BLUSH.copy(alpha = blushA), Offset(127f, 132.5f), Size(24f, 9f))

                        if (mood == NikaMood.HAPPY) {
                            happyEye(74f); happyEye(126f)
                        } else {
                            eye(74f, -1f, blink.value, look)
                            eye(126f, 1f, blink.value, look)
                        }
                        mouth(mood, mouth.value)

                        fillStroke(HAIR_CAP, HAIR, HAIR_LINE)
                        drawPath(SHINE_1, SHINE, style = Stroke(3f, cap = StrokeCap.Round))
                        drawPath(SHINE_2, SHINE, style = Stroke(3f, cap = StrokeCap.Round))

                        if (mood == NikaMood.THINK) thinkingDots(fast)
                    }
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

/** Глаз: белок, радужка с бликами и «стрелка» ресниц снаружи. side = -1 левый, 1 правый. */
private fun DrawScope.eye(cx: Float, side: Float, blink: Float, look: Float) {
    val open = 1f - blink * 0.92f
    scale(1f, open, pivot = Offset(cx, 112f)) {
        val lash = Path().apply {
            moveTo(cx + side * 15f, 101f); lineTo(cx + side * 27f, 93f); lineTo(cx + side * 11f, 97f); close()
        }
        drawPath(lash, Color(0xFF111111))
        drawOval(Color.White, Offset(cx - 17f, 94f), Size(34f, 36f))
        drawOval(Color(0xFF111111), Offset(cx - 17f, 94f), Size(34f, 36f), style = Stroke(1.6f))
        val ix = cx - side // зрачки чуть ближе к носу
        val iy = 114f + look * 3.5f
        drawCircle(Color(0xFF2B2B2B), 12.5f, Offset(ix, iy))
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
        open > 0.05f -> {
            val h = 3f + open * 10f
            drawOval(Color(0xFF8E2547), Offset(92f, 146f - h / 2f), Size(16f, h))
            drawOval(LIPS, Offset(92f, 146f - h / 2f - 1.5f), Size(16f, 3.5f))
        }
        else -> {
            val smile = if (mood == NikaMood.HAPPY) SMILE_BIG else SMILE
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

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

private val HAIR = Color(0xFF1631A8)
private val HAIR_LINE = Color(0xFF0E1F6E)
private val SHINE = Color(0xB33D63E0)
private val SKIN = Color(0xFFF7C8A0)
private val OUTLINE = Color(0xFF3B2F2A)
private val BLUSH = Color(0xFFF06292)
private val SUIT = Color(0xFF1E5FAF)
private val SUIT_TOP = Color(0xFF2368B5)
private val SUIT_LINE = Color(0xFF173F75)
private val SHOES = Color(0xFF222222)
private val LIPS = Color(0xFFE86A9A)

private val LOCK_L = svg("M46,116 C30,124 22,160 24,198 C25,232 30,252 46,254 C60,256 66,246 64,232 C61,208 64,176 70,148 Z")
private val LOCK_R = svg("M154,116 C170,124 178,160 176,198 C175,232 170,252 154,254 C140,256 134,246 136,232 C139,208 136,176 130,148 Z")
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
