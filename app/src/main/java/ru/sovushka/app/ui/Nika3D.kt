package ru.sovushka.app.ui

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import ru.sovushka.app.R
import ru.sovushka.app.graph
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ника. По умолчанию — живая объёмная (вектор со светотенью, двигается всем телом),
 * в настройках можно включить неподвижные 3D-картинки из OpenArt. Размеры у обеих одинаковые (200 × 320).
 */
@Composable
fun Nika(mood: NikaMood, modifier: Modifier = Modifier, talking: Boolean = mood == NikaMood.TALK) {
    val g = LocalContext.current.graph
    val prefs by g.settings.prefs.collectAsState()
    if (prefs.nikaStatic) Nika3D(mood, modifier, talking) else NikaVector(mood, modifier, talking)
}

/** Картинки поз декодируются один раз на всё приложение. */
private object NikaPoses {
    private val cache = HashMap<Int, ImageBitmap>()

    fun get(res: Resources, id: Int): ImageBitmap = cache.getOrPut(id) {
        BitmapFactory.decodeResource(res, id).asImageBitmap()
    }

    fun id(mood: NikaMood, talking: Boolean, strict: Boolean): Int {
        val pose = when (mood) {
            NikaMood.TALK -> if (talking) 1 else 0
            NikaMood.IDLE -> if (talking) 1 else 0
            NikaMood.HAPPY, NikaMood.DANCE -> 2
            NikaMood.SAD -> 3
            NikaMood.LISTEN -> 4
            NikaMood.THINK -> 5
            NikaMood.WAVE -> 6
            NikaMood.SURPRISED -> 7
        }
        return (if (strict) STRICT else CASUAL)[pose]
    }

    private val CASUAL = intArrayOf(
        R.drawable.nika3d_idle, R.drawable.nika3d_talk, R.drawable.nika3d_happy, R.drawable.nika3d_sad,
        R.drawable.nika3d_listen, R.drawable.nika3d_think, R.drawable.nika3d_wave, R.drawable.nika3d_surprised,
    )
    private val STRICT = intArrayOf(
        R.drawable.nika3d_math_idle, R.drawable.nika3d_math_talk, R.drawable.nika3d_math_happy, R.drawable.nika3d_math_sad,
        R.drawable.nika3d_math_listen, R.drawable.nika3d_math_think, R.drawable.nika3d_math_wave, R.drawable.nika3d_math_surprised,
    )
}

/**
 * 3D-Ника: позы меняются плавным перетеканием с «пружинкой», а живость дают трансформации —
 * дыхание, покачивание, подскоки от радости, танец, кивки во время речи и случайные движения в покое.
 */
@Composable
private fun Nika3D(mood: NikaMood, modifier: Modifier, talking: Boolean) {
    val res = LocalContext.current.resources
    val strict = LocalNikaOutfit.current == NikaOutfit.STRICT
    val loop = rememberInfiniteTransition(label = "nika3d")
    val phase by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart), label = "breath")
    val fast by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Restart), label = "fast")

    // «Пружинка» при смене настроения.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(mood) {
        pop.snapTo(if (mood == NikaMood.SURPRISED) 1.1f else 0.94f)
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f))
    }

    // Кивки во время речи: небольшие случайные «толчки».
    val nod = remember { Animatable(0f) }
    LaunchedEffect(talking) {
        if (!talking) { nod.animateTo(0f, tween(150)); return@LaunchedEffect }
        while (true) {
            nod.animateTo(Random.nextFloat() * 0.8f + 0.2f, tween(140))
            nod.animateTo(Random.nextFloat() * 0.2f, tween(160))
        }
    }

    // В покое иногда подпрыгивает или наклоняет голову.
    val hop = remember { Animatable(0f) }
    val tiltQ = remember { Animatable(0f) }
    LaunchedEffect(mood, talking) {
        if (mood != NikaMood.IDLE || talking) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(2600, 5600))
            if (Random.nextBoolean()) {
                hop.animateTo(1f, tween(170)); hop.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 300f))
            } else {
                val t = if (Random.nextBoolean()) 4f else -4f
                tiltQ.animateTo(t, tween(380)); delay(700); tiltQ.animateTo(0f, tween(420))
            }
        }
    }

    val id = NikaPoses.id(mood, talking, strict)
    Box(modifier.aspectRatio(200f / 320f), contentAlignment = Alignment.BottomCenter) {
        Crossfade(targetState = id, animationSpec = tween(180), label = "pose") { pose ->
            val bmp = remember(pose) { NikaPoses.get(res, pose) }
            Image(
                bmp, null,
                Modifier.fillMaxSize().graphicsLayer {
                    val b = sin(phase * 2 * PI).toFloat()
                    val air = abs(sin(fast * PI).toFloat())
                    val dance = sin(fast * 2 * PI).toFloat()
                    val h = size.height
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    val sx = 1f + b * 0.006f
                    var sy = 1f + b * 0.012f
                    var ty = -hop.value * h * 0.04f
                    var rot = tiltQ.value
                    when (mood) {
                        NikaMood.HAPPY -> { ty -= air * h * 0.05f; sy *= 1f + (1f - air) * -0.03f }
                        NikaMood.DANCE -> { ty -= air * h * 0.03f; rot += dance * 6f }
                        NikaMood.WAVE -> rot += sin(phase * 4 * PI).toFloat() * 2.5f
                        NikaMood.THINK -> rot += sin(phase * 2 * PI).toFloat() * 1.5f
                        NikaMood.SAD -> { ty += h * 0.01f; sy *= 0.98f }
                        else -> {}
                    }
                    if (talking) { ty -= nod.value * h * 0.012f; rot += (nod.value - 0.5f) * 1.6f }
                    scaleX = sx * pop.value
                    scaleY = sy * pop.value
                    translationY = ty
                    rotationZ = rot
                },
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter,
            )
        }
    }
}
