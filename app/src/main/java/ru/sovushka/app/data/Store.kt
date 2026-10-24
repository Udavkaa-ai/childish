package ru.sovushka.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.sovushka.core.AppData
import java.io.File

/** Все уроки, слова и прогресс — один JSON-файл во внутренней памяти телефона. */
class Store(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
    private val mutex = Mutex()
    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data
    val value: AppData get() = _data.value

    private fun load(): AppData = try {
        if (file.exists()) json.decodeFromString<AppData>(file.readText()) else AppData()
    } catch (e: Exception) {
        // Повреждённый файл не теряем: откладываем рядом и начинаем с чистого листа.
        file.copyTo(File(file.parentFile, file.name + ".broken-" + System.currentTimeMillis()), overwrite = true)
        AppData()
    }

    suspend fun update(f: (AppData) -> AppData) = mutex.withLock {
        val next = f(_data.value)
        _data.value = next
        withContext(Dispatchers.IO) { write(next) }
    }

    private fun write(d: AppData) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), d))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText()); tmp.delete()
        }
    }

    fun exportJson(): String = json.encodeToString(AppData.serializer(), _data.value)

    /** Восстановить из резервной копии. Бросает исключение, если файл не наш. */
    suspend fun importJson(text: String) {
        val d = json.decodeFromString<AppData>(text)
        update { d }
    }
}
