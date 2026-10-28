package ru.sovushka.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import ru.sovushka.app.data.Prefs
import ru.sovushka.app.graph
import ru.sovushka.core.KeyTransfer
import java.io.File
import java.util.concurrent.Executors

/**
 * Камера внутри приложения. Системная камера — отдельное приложение, и родительский контроль
 * ограничивает её по времени; своя камера работает, пока открыта Ника.
 *
 * Режим фото: можно снять несколько страниц подряд, каждая уходит в onPhoto; «Готово» закрывает.
 * Режим QR (onCode != null): ищет QR-код и отдаёт его текст один раз.
 */
@Composable
fun CameraScreen(
    onClose: () -> Unit,
    title: String = "Наведи на страницу и нажми на кружок",
    multiple: Boolean = true,
    onPhoto: (Uri) -> Unit = {},
    onCode: ((String) -> Unit)? = null,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val ctx = LocalContext.current
        val owner = LocalLifecycleOwner.current
        var granted by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        }
        val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            granted = ok
            if (!ok) {
                Toast.makeText(ctx, "Без разрешения на камеру снимать не получится", Toast.LENGTH_LONG).show()
                onClose()
            }
        }
        LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.CAMERA) }

        val capture = remember {
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
        }
        var shots by remember { mutableIntStateOf(0) }
        var busy by remember { mutableStateOf(false) }
        var flash by remember { mutableIntStateOf(0) }
        val analyzer = remember { Executors.newSingleThreadExecutor() }
        DisposableEffect(Unit) {
            onDispose {
                runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() }
                analyzer.shutdown()
            }
        }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (granted) {
                AndroidView(
                    factory = { c ->
                        PreviewView(c).also { view ->
                            val future = ProcessCameraProvider.getInstance(c)
                            future.addListener({
                                runCatching {
                                    val provider = future.get()
                                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                                    provider.unbindAll()
                                    if (onCode != null) {
                                        provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, qrAnalysis(analyzer, onCode))
                                    } else {
                                        provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                                    }
                                }.onFailure {
                                    Toast.makeText(c, "Камера не открылась: ${it.message}", Toast.LENGTH_LONG).show()
                                    onClose()
                                }
                            }, ContextCompat.getMainExecutor(c))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (flash > 0) {
                LaunchedEffect(flash) { kotlinx.coroutines.delay(120); flash = 0 }
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.7f)))
            }
            if (onCode != null) {
                Box(Modifier.align(Alignment.Center).size(260.dp).border(3.dp, Color.White, RoundedCornerShape(24.dp)))
            }
            Text(
                if (shots > 0) "$title\nСнято страниц: $shots" else title,
                color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CamButton(if (shots > 0) "Готово" else "Закрыть") { onClose() }
                if (onCode == null) {
                    Box(
                        Modifier.size(82.dp).border(5.dp, Color.White, CircleShape).padding(8.dp)
                            .background(if (busy) Color.Gray else Color.White, CircleShape)
                            .clickable(enabled = !busy && granted) {
                                busy = true
                                val dir = File(ctx.cacheDir, "camera").apply { mkdirs() }
                                val file = File(dir, "shot-${System.currentTimeMillis()}.jpg")
                                capture.takePicture(
                                    ImageCapture.OutputFileOptions.Builder(file).build(),
                                    ContextCompat.getMainExecutor(ctx),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            busy = false
                                            flash++
                                            shots++
                                            onPhoto(Uri.fromFile(file))
                                            if (!multiple) onClose()
                                        }

                                        override fun onError(e: ImageCaptureException) {
                                            busy = false
                                            Toast.makeText(ctx, "Не получилось снять: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                )
                            },
                    )
                }
                Box(Modifier.size(width = 96.dp, height = 1.dp))
            }
        }
    }
}

@Composable
private fun CamButton(text: String, onClick: () -> Unit) {
    Column(
        Modifier.background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50)).clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) { Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

/** Анализ кадров на QR: высокое разрешение — код с ключом плотный. Отдаёт первый найденный код один раз. */
@OptIn(ExperimentalGetImage::class)
private fun qrAnalysis(executor: java.util.concurrent.Executor, onCode: (String) -> Unit): ImageAnalysis {
    val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    val main = android.os.Handler(android.os.Looper.getMainLooper())
    var done = false
    val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setResolutionSelector(
            ResolutionSelector.Builder().setResolutionStrategy(
                ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
            ).build(),
        )
        .build()
    analysis.setAnalyzer(executor) { proxy ->
        val image = proxy.image
        if (image == null || done) {
            proxy.close()
            return@setAnalyzer
        }
        scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { codes ->
                val text = codes.firstNotNullOfOrNull { it.rawValue?.takeIf { v -> v.isNotBlank() } }
                if (text != null && !done) {
                    done = true
                    main.post { onCode(text) }
                }
            }
            .addOnCompleteListener { proxy.close() }
    }
    return analysis
}

/**
 * Кнопка «ключи с компьютера»: сканирует QR со страницы tools/nika-keys.html
 * и сохраняет пришедшие ключи (пустые поля не трогают то, что уже настроено).
 */
@Composable
fun ScanKeysButton(text: String = "📷 Сканировать QR с компьютера", modifier: Modifier = Modifier, brush: androidx.compose.ui.graphics.Brush = Palette.pinkish) {
    val ctx = LocalContext.current
    val g = ctx.graph
    var open by remember { mutableStateOf(false) }
    PillButton(text, modifier, brush = brush) { open = true }
    if (open) {
        CameraScreen(
            onClose = { open = false },
            title = "Наведи камеру на QR-код на экране компьютера",
            onCode = { raw ->
                open = false
                val k = KeyTransfer.parse(raw)
                if (k == null) {
                    Toast.makeText(ctx, "Это не QR с ключами для Ники", Toast.LENGTH_LONG).show()
                } else {
                    g.settings.update { p ->
                        p.copy(
                            meshToken = k.mesh.ifBlank { p.meshToken },
                            meshStudentId = if (k.mesh.isNotBlank()) -1 else p.meshStudentId,
                            gigaKey = k.gigaKey.ifBlank { p.gigaKey },
                            yaKey = k.yaKey.ifBlank { p.yaKey },
                            yaFolder = k.yaFolder.ifBlank { p.yaFolder },
                            provider = when {
                                p.provider != Prefs.PROVIDER_NONE -> p.provider
                                k.gigaKey.isNotBlank() -> Prefs.PROVIDER_GIGACHAT
                                k.yaKey.isNotBlank() && k.yaFolder.isNotBlank() -> Prefs.PROVIDER_YANDEX
                                else -> p.provider
                            },
                        )
                    }
                    Toast.makeText(ctx, "Готово! Получено: ${k.describe()}", Toast.LENGTH_LONG).show()
                }
            },
        )
    }
}
