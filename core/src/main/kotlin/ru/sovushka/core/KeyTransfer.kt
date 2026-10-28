package ru.sovushka.core

import ru.sovushka.core.mesh.MeshClient
import java.net.URLDecoder

/**
 * Ключи с компьютера через QR-код (страница tools/nika-keys.html).
 * Формат: «NIKA1:mesh=…&gigaKey=…&yaKey=…&yaFolder=…», пустые поля не передаются.
 * Подходит и QR, где лежит просто токен МЭШ.
 */
object KeyTransfer {
    const val PREFIX = "NIKA1:"

    data class Keys(
        val mesh: String = "",
        val gigaKey: String = "",
        val yaKey: String = "",
        val yaFolder: String = "",
    ) {
        val isEmpty: Boolean get() = mesh.isBlank() && gigaKey.isBlank() && yaKey.isBlank() && yaFolder.isBlank()

        /** Что пришло — для сообщения родителю. */
        fun describe(): String = listOfNotNull(
            "ключ МЭШ".takeIf { mesh.isNotBlank() },
            "ключ GigaChat".takeIf { gigaKey.isNotBlank() },
            "ключ Yandex Cloud".takeIf { yaKey.isNotBlank() },
            "каталог Yandex Cloud".takeIf { yaFolder.isNotBlank() },
        ).joinToString(", ")
    }

    /** null — это не наш QR (или токен МЭШ в нём испорчен). */
    fun parse(raw: String): Keys? {
        val s = raw.trim()
        if (!s.startsWith(PREFIX)) return if (MeshClient.looksLikeToken(s)) Keys(mesh = s) else null
        val map = s.removePrefix(PREFIX).split('&').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) null else part.substring(0, i) to runCatching { URLDecoder.decode(part.substring(i + 1), "UTF-8") }.getOrDefault("").trim()
        }.toMap()
        val keys = Keys(map["mesh"].orEmpty(), map["gigaKey"].orEmpty(), map["yaKey"].orEmpty(), map["yaFolder"].orEmpty())
        if (keys.mesh.isNotBlank() && !MeshClient.looksLikeToken(keys.mesh)) return null
        return keys.takeUnless { it.isEmpty }
    }
}
