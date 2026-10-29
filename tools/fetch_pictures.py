"""
Картинки для букваря из ARASAAC (https://arasaac.org) — набор пиктограмм для обучения детей.
Лицензия CC BY-NC-SA 4.0: «Pictographic symbols are the property of the Government of Aragón
and have been created by Sergio Palao for ARASAAC». Только некоммерческое использование.

Скрипт берёт слова из core/.../abc/Abc.kt (и запасные слова), ищет каждое по-русски,
скачивает первую подходящую цветную картинку 300×300 и собирает листы для проверки глазами.
Запускается в GitHub Actions (workflow «Картинки ARASAAC»).
"""
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "arasaac"
PNG = OUT / "png"
API = "https://api.arasaac.org/v1/pictograms/ru"
STATIC = "https://static.arasaac.org/pictograms/{id}/{id}_300.png"

# Слова, которые хорошо бы вернуть в букварь, если найдётся картинка.
EXTRA = ["ведро", "пила", "окно", "муха", "тюлень", "мак", "игла", "иголка", "ирис", "усы", "юбка", "юла",
         "утюг", "иней", "ива", "игрушки", "индейка", "лук", "мука", "нож", "носок", "дыня", "каша", "рама",
         "сани", "сумка", "лужа", "пила", "лимон", "ухо", "юнга", "яма", "якорь", "эхо", "этаж", "шуба"]


def words() -> list[str]:
    src = (ROOT / "core/src/main/kotlin/ru/sovushka/core/abc/Abc.kt").read_text(encoding="utf-8")
    found = re.findall(r'p\("([^"]+)", "[^"]+"\)', src)
    seen, out = set(), []
    for w in found + EXTRA:
        key = w.lower()
        if key not in seen:
            seen.add(key)
            out.append(w)
    return out


def get_json(url: str):
    req = urllib.request.Request(url, headers={"User-Agent": "nika-abc/1.0"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.load(r)
    except Exception as e:  # 404 — ничего не нашлось
        print(f"  {url}: {e}", file=sys.stderr)
        return []


def pick(word: str):
    q = urllib.parse.quote(word.lower())
    for kind in ("bestsearch", "search"):
        res = get_json(f"{API}/{kind}/{q}")
        good = [p for p in res if not p.get("schematic") and not p.get("sex") and not p.get("violence")]
        if good:
            return good[0], [p["_id"] for p in good[1:6]]
    return None, []


# Слова, где первая картинка не подошла или не нашлась: качаем варианты, выбираем глазами.
ALTS = {
    "мышка": ["мышь", "мышка"], "рак": ["рак", "раки"], "ведро": ["ведро", "ведёрко"],
    "конь": ["лошадь", "конь"], "очки": ["очки"], "пила": ["пила", "ножовка"], "пони": ["пони"],
    "зима": ["зима", "снег"], "осьминог": ["осьминог"], "морковка": ["морковь"], "индюк": ["индюк", "индейка"],
    "ёжик": ["ёж", "еж"], "эскимо": ["мороженое"], "цирк": ["цирк"], "паук": ["паук"], "зайка": ["заяц", "кролик"],
    "вагон": ["вагон", "поезд"], "земляника": ["клубника", "земляника"], "щенок": ["щенок"], "хомяк": ["хомяк"],
}


def alternatives():
    alt = OUT / "alt"
    alt.mkdir(parents=True, exist_ok=True)
    found = {}
    for word, queries in ALTS.items():
        ids = []
        for q in queries:
            res = get_json(f"{API}/search/{urllib.parse.quote(q)}")
            ids += [p["_id"] for p in res if not p.get("sex") and not p.get("violence")][:6]
        for pid in list(dict.fromkeys(ids))[:8]:
            req = urllib.request.Request(STATIC.format(id=pid), headers={"User-Agent": "nika-abc/1.0"})
            try:
                with urllib.request.urlopen(req, timeout=60) as r:
                    (alt / f"{word}__{pid}.png").write_bytes(r.read())
                found.setdefault(word, []).append(pid)
            except Exception as e:
                print(f"  не скачалась {word} {pid}: {e}", file=sys.stderr)
            time.sleep(0.15)
    (OUT / "alt.json").write_text(json.dumps(found, ensure_ascii=False, indent=1), encoding="utf-8")
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 18)
    except OSError:
        font = ImageFont.load_default()
    cell, cols = 170, 8
    words = list(found)
    for n in range(0, len(words), 5):
        chunk = words[n:n + 5]
        sheet = Image.new("RGB", (cols * cell, len(chunk) * (cell + 30)), "white")
        d = ImageDraw.Draw(sheet)
        for r, w in enumerate(chunk):
            for c, pid in enumerate(found[w][:cols]):
                x, y = c * cell, r * (cell + 30)
                im = Image.open(alt / f"{w}__{pid}.png").convert("RGBA").resize((cell - 16, cell - 16))
                bg = Image.new("RGBA", im.size, "white")
                sheet.paste(Image.alpha_composite(bg, im).convert("RGB"), (x + 8, y + 4))
                d.text((x + cell // 2, y + cell + 10), f"{w} {pid}", fill="black", font=font, anchor="mm")
        sheet.save(OUT / f"alt_{n // 5 + 1}.png")


def main():
    if "--alts" in sys.argv:
        alternatives()
        return
    PNG.mkdir(parents=True, exist_ok=True)
    index = {}
    for w in words():
        p, more = pick(w)
        if p is None:
            print(f"нет: {w}")
            continue
        pid = p["_id"]
        kw = [k.get("keyword", "") for k in p.get("keywords", [])][:3]
        req = urllib.request.Request(STATIC.format(id=pid), headers={"User-Agent": "nika-abc/1.0"})
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                (PNG / f"{w.lower()}.png").write_bytes(r.read())
        except Exception as e:
            print(f"  не скачалась {w}: {e}", file=sys.stderr)
            continue
        index[w.lower()] = {"id": pid, "keywords": kw, "alternatives": more}
        print(f"{w}: {pid} {kw}")
        time.sleep(0.2)
    (OUT / "index.json").write_text(json.dumps(index, ensure_ascii=False, indent=1), encoding="utf-8")
    sheets(index)


def sheets(index: dict):
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 22)
    except OSError:
        font = ImageFont.load_default()
    items = sorted(index)
    per, cols, cell = 40, 8, 200
    for n in range(0, len(items), per):
        chunk = items[n:n + per]
        rows = (len(chunk) + cols - 1) // cols
        sheet = Image.new("RGB", (cols * cell, rows * (cell + 34)), "white")
        d = ImageDraw.Draw(sheet)
        for i, w in enumerate(chunk):
            x, y = (i % cols) * cell, (i // cols) * (cell + 34)
            im = Image.open(PNG / f"{w}.png").convert("RGBA").resize((cell - 20, cell - 20))
            bg = Image.new("RGBA", im.size, "white")
            sheet.paste(Image.alpha_composite(bg, im).convert("RGB"), (x + 10, y + 6))
            d.text((x + cell // 2, y + cell + 12), w, fill="black", font=font, anchor="mm")
        sheet.save(OUT / f"sheet_{n // per + 1}.png")


if __name__ == "__main__":
    main()
