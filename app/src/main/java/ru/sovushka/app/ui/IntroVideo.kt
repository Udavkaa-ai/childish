package ru.sovushka.app.ui

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ru.sovushka.app.R
import ru.sovushka.app.graph

/**
 * Видео-заставка «Ника ведёт в мир учёбы» (сделано в Gemini по начальному кадру из OpenArt).
 * Играет на TextureView — так работают скругления и прокрутка. autoPlay — сразу запустить.
 */
@Composable
fun NikaIntroVideo(autoPlay: Boolean, modifier: Modifier = Modifier, onStart: () -> Unit = {}) {
    val g = LocalContext.current.graph
    var playing by remember { mutableStateOf(autoPlay) }
    Box(
        modifier.fillMaxWidth().aspectRatio(16f / 9f)
            .glass(RoundedCornerShape(22.dp), Color.White.copy(alpha = 0.85f), rim = 3.dp)
            .padding(4.dp).clip(RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (playing) {
            val holder = remember { arrayOfNulls<MediaPlayer>(1) }
            AndroidView(
                factory = { ctx ->
                    g.speaker.stop()
                    onStart()
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                val mp = MediaPlayer()
                                holder[0] = mp
                                runCatching {
                                    mp.setDataSource(ctx, Uri.parse("android.resource://${ctx.packageName}/${R.raw.nika_intro}"))
                                    mp.setSurface(Surface(st))
                                    mp.setOnPreparedListener { it.start() }
                                    mp.setOnCompletionListener { playing = false }
                                    mp.setOnErrorListener { _, _, _ -> playing = false; true }
                                    mp.prepareAsync()
                                }.onFailure { playing = false }
                            }

                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                holder[0]?.let { runCatching { it.stop(); it.release() } }
                                holder[0] = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxSize().clickable { playing = false },
                onRelease = {
                    holder[0]?.let { runCatching { it.stop(); it.release() } }
                    holder[0] = null
                },
            )
        } else {
            Image(
                painterResource(R.drawable.nika_intro_poster), "Видео: Ника ведёт в мир учёбы",
                Modifier.fillMaxSize().clickable { playing = true },
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier.size(64.dp).glass(CircleShape, Color.White.copy(alpha = 0.75f), rim = 2.dp).clickable { playing = true },
                contentAlignment = Alignment.Center,
            ) { AppIcon(AppIcons.PLAY, size = 40.dp) }
        }
    }
}
