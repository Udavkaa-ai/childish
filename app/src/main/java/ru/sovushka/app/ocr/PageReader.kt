package ru.sovushka.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Фото страницы учебника: сохранить уменьшенную копию и распознать текст прямо на телефоне (ML Kit). */
class PageReader(private val context: Context) {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val photosDir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    /** Скопировать фото к себе: повернуть по EXIF, уменьшить до 2000 px, сохранить JPEG. */
    suspend fun importPhoto(uri: Uri): File = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2600) sample *= 2
        val raw = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Не удалось открыть фото")
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scale = minOf(1f, 2000f / maxOf(raw.width, raw.height))
        val m = Matrix().apply { postRotate(rotation); postScale(scale, scale) }
        val bmp = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
        val file = File(photosDir, UUID.randomUUID().toString() + ".jpg")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (bmp !== raw) raw.recycle()
        bmp.recycle()
        file
    }

    /** Фото ребёнка для аватарки: квадрат по центру 512×512 с учётом поворота. */
    suspend fun importAvatar(uri: Uri, target: File) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / sample > 1400) sample *= 2
        val raw = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Не удалось открыть фото")
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val side = minOf(raw.width, raw.height)
        val scale = 512f / side
        val m = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
        val square = Bitmap.createBitmap(raw, (raw.width - side) / 2, (raw.height - side) / 2, side, side, m, true)
        target.parentFile?.mkdirs()
        target.outputStream().use { square.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        if (square !== raw) raw.recycle()
        square.recycle()
    }

    suspend fun recognize(file: File): String {
        val bmp = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.path) } ?: return ""
        return try {
            suspendCancellableCoroutine { cont ->
                recognizer.process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener { cont.resume(it.text) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        } finally {
            bmp.recycle()
        }
    }

    /** Маленькая картинка для списка. */
    suspend fun thumbnail(path: String, maxSide: Int = 300): Bitmap? = withContext(Dispatchers.IO) {
        val f = File(path)
        if (!f.exists()) return@withContext null
        val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, b)
        var s = 1
        while (maxOf(b.outWidth, b.outHeight) / s > maxSide * 2) s *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })
    }
}
