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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.sovushka.app.graph

/** Большая градиентная кнопка с эмодзи — основной элемент навигации. */
@Composable
fun BigButton(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    brush: Brush = Palette.button,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(if (enabled) brush else Brush.linearGradient(listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E))))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 34.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
        }
    }
}

/** Обычная кнопка поменьше. */
@Composable
fun PillButton(text: String, modifier: Modifier = Modifier, brush: Brush = Palette.button, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) brush else Brush.linearGradient(listOf(Color(0xFFCFCFCF), Color(0xFFBDBDBD))))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 26.sp, color = Palette.Pink, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Ника с облачком-репликой. */
@Composable
fun NikaSays(text: String, mood: NikaMood, talking: Boolean, modifier: Modifier = Modifier, nikaHeight: Dp = 170.dp) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Nika(mood = mood, talking = talking, modifier = Modifier.height(nikaHeight))
        Spacer(Modifier.width(8.dp))
        if (text.isNotBlank()) {
            Box(
                Modifier
                    .weight(1f)
                    .shadow(3.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(14.dp),
            ) { Text(text, style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

/** Белая карточка-подложка. */
@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.Card)
            .padding(16.dp),
    ) { content() }
}

enum class ChoiceState { NORMAL, RIGHT, WRONG, DIM }

@Composable
fun ChoiceButton(text: String, state: ChoiceState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = when (state) {
        ChoiceState.RIGHT -> Palette.Good
        ChoiceState.WRONG -> Palette.Bad
        ChoiceState.DIM -> Color(0xFFF1EEF6)
        ChoiceState.NORMAL -> Color.White
    }
    val fg = if (state == ChoiceState.RIGHT || state == ChoiceState.WRONG) Color.White else Palette.Ink
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(2.dp, if (state == ChoiceState.NORMAL) Palette.PinkLight else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable(enabled = state == ChoiceState.NORMAL, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center) }
}

/** Круглая кнопка микрофона, пульсирует, пока слушает. */
@Composable
fun MicButton(listening: Boolean, level: Float, size: Dp = 96.dp, label: String = "🎤", onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "mic")
    val p by pulse.animateFloat(1f, 1.12f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "p")
    val scale = if (listening) p + level * 0.15f else 1f
    Box(
        Modifier
            .size(size)
            .scale(scale)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(if (listening) Brush.linearGradient(listOf(Palette.Bad, Palette.Pink)) else Palette.button)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = (size.value * 0.42f).sp) }
}

@Composable
fun Stars(count: Int, max: Int = 3, size: Int = 44) {
    Row(horizontalArrangement = Arrangement.Center) {
        repeat(max) { i -> Text(if (i < count) "⭐" else "☆", fontSize = size.sp, color = Color(0xFFFFC107)) }
    }
}

@Composable
fun SoundButton(onClick: () -> Unit, slow: Boolean = false) {
    Box(
        Modifier.size(56.dp).clip(CircleShape).background(if (slow) Palette.bluish else Palette.pinkish).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(if (slow) "🐢" else "🔊", fontSize = 26.sp) }
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
        modifier.size(size).shadow(4.dp, CircleShape).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)),
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
