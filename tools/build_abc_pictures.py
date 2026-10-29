"""
Собирает картинки букваря в приложение: art/arasaac → res/drawable-nodpi/abc_*.webp
и карту «слово → картинка» в AbcPictures.kt. Слова без хорошей картинки остаются с эмодзи.
"""
import json
import re
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "art" / "arasaac"
RES = ROOT / "app/src/main/res/drawable-nodpi"
KT = ROOT / "app/src/main/java/ru/sovushka/app/ui/AbcPictures.kt"

# Выбранные глазами варианты вместо первой найденной картинки (номер ARASAAC).
CHOSEN = {
    "мышка": 2546, "рак": 11200, "ведро": 2725, "конь": 2294, "очки": 3329, "пила": 2799, "пони": 8324,
    "зима": 2884, "осьминог": 3379, "морковка": 2619, "индюк": 2509, "эскимо": 35209, "цирк": 6450,
    "паук": 38275, "зайка": 2351, "вагон": 7282, "земляника": 2400,
}
# Картинки, которые не подошли, — для этих слов остаются эмодзи.
SKIP = {"ёжик"}

LAT = dict(zip("абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
               ["a", "b", "v", "g", "d", "e", "yo", "zh", "z", "i", "j", "k", "l", "m", "n", "o", "p", "r", "s", "t",
                "u", "f", "h", "c", "ch", "sh", "sch", "", "y", "", "e", "yu", "ya"]))


def name(word: str) -> str:
    return "abc_" + "".join(LAT.get(c, "_") for c in word.lower()).replace("-", "_")


def main():
    src = (ROOT / "core/src/main/kotlin/ru/sovushka/core/abc/Abc.kt").read_text(encoding="utf-8")
    words = list(dict.fromkeys(w.lower() for w in re.findall(r'p\("([^"]+)", "[^"]+"\)', src)))
    for old in RES.glob("abc_*.webp"):
        old.unlink()
    entries = []
    for w in words:
        if w in SKIP:
            continue
        if w in CHOSEN:
            path = ART / "alt" / f"{w}__{CHOSEN[w]}.png"
        else:
            path = ART / "png" / f"{w}.png"
        if not path.exists():
            print("нет картинки:", w)
            continue
        im = Image.open(path).convert("RGBA")
        im.thumbnail((256, 256), Image.LANCZOS)
        n = name(w)
        im.save(RES / f"{n}.webp", "WEBP", quality=88, method=6)
        entries.append((w, n))
    lines = ",\n".join(f'    "{w}" to R.drawable.{n}' for w, n in entries)
    KT.write_text(
        "package ru.sovushka.app.ui\n\n"
        "import ru.sovushka.app.R\n\n"
        "/**\n"
        " * Картинки букваря из ARASAAC (https://arasaac.org, CC BY-NC-SA 4.0): Pictographic symbols are\n"
        " * the property of the Government of Aragón and have been created by Sergio Palao for ARASAAC.\n"
        " * Файл собирает tools/build_abc_pictures.py — руками не править.\n"
        " */\n"
        "internal val ABC_PICTURES: Map<String, Int> = mapOf(\n" + lines + ",\n)\n",
        encoding="utf-8",
    )
    print(len(entries), "картинок")


if __name__ == "__main__":
    main()
