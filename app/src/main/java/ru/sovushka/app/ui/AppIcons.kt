package ru.sovushka.app.ui

/** Набор иконок приложения в одном стиле: пастельная заливка, белые блики, тёмный скруглённый контур (сетка 48×48). */
object AppIcons {
    val CAMERA = IconSpec(
        fill = listOf("M9,16 H39 A5,5 0 0 1 44,21 V36 A5,5 0 0 1 39,41 H9 A5,5 0 0 1 4,36 V21 A5,5 0 0 1 9,16 Z", "M17,16 L20,10 H28 L31,16 Z"),
        accent = listOf("M16.5,28 a7.5,7.5 0 1,0 15.0,0 a7.5,7.5 0 1,0 -15.0,0 Z"),
        dot = listOf("M35.7,21.5 a1.8,1.8 0 1,0 3.6,0 a1.8,1.8 0 1,0 -3.6,0 Z"),
        line = listOf("M9,16 H39 A5,5 0 0 1 44,21 V36 A5,5 0 0 1 39,41 H9 A5,5 0 0 1 4,36 V21 A5,5 0 0 1 9,16 Z", "M17,16 L20,10 H28 L31,16", "M16.5,28 a7.5,7.5 0 1,0 15.0,0 a7.5,7.5 0 1,0 -15.0,0 Z"),
        tint = Pastel.Pink,
    )
    val REPEAT = IconSpec(
        fill = listOf("M7,24 a17,17 0 1,0 34,0 a17,17 0 1,0 -34,0 Z"),
        accent = listOf(),
        dot = listOf(),
        line = listOf("M36.5,19 A13.5,13.5 0 0 0 12,16.5", "M11.5,9.5 V17 H19", "M11.5,29 A13.5,13.5 0 0 0 36,31.5", "M36.5,38.5 V31 H29"),
        tint = Pastel.Blue,
    )
    val BOOKS = IconSpec(
        fill = listOf("M7,31 H39 V40 H7 Z", "M10,22 H40 V31 H10 Z", "M8,13 H36 V22 H8 Z"),
        accent = listOf("M12,34 H34 V37 H12 Z"),
        dot = listOf(),
        line = listOf("M7,31 H39 V40 H7 Z", "M10,22 H40 V31 H10 Z", "M8,13 H36 V22 H8 Z", "M14,13 V22", "M33,22 V31"),
        tint = Pastel.Yellow,
    )
    val CHAT = IconSpec(
        fill = listOf("M11,8 H37 A7,7 0 0 1 44,15 V27 A7,7 0 0 1 37,34 H24 L15,41 V34 H11 A7,7 0 0 1 4,27 V15 A7,7 0 0 1 11,8 Z"),
        accent = listOf(),
        dot = listOf("M12.6,21 a2.4,2.4 0 1,0 4.8,0 a2.4,2.4 0 1,0 -4.8,0 Z", "M21.6,21 a2.4,2.4 0 1,0 4.8,0 a2.4,2.4 0 1,0 -4.8,0 Z", "M30.6,21 a2.4,2.4 0 1,0 4.8,0 a2.4,2.4 0 1,0 -4.8,0 Z"),
        line = listOf("M11,8 H37 A7,7 0 0 1 44,15 V27 A7,7 0 0 1 37,34 H24 L15,41 V34 H11 A7,7 0 0 1 4,27 V15 A7,7 0 0 1 11,8 Z"),
        tint = Pastel.Mint,
    )
    val BACKPACK = IconSpec(
        fill = listOf("M11,19 A9,9 0 0 1 20,10 H28 A9,9 0 0 1 37,19 V39 A4,4 0 0 1 33,43 H15 A4,4 0 0 1 11,39 Z"),
        accent = listOf("M16,28 H32 V37 H16 Z"),
        dot = listOf(),
        line = listOf("M11,19 A9,9 0 0 1 20,10 H28 A9,9 0 0 1 37,19 V39 A4,4 0 0 1 33,43 H15 A4,4 0 0 1 11,39 Z", "M19,10 V7 A2,2 0 0 1 21,5 H27 A2,2 0 0 1 29,7 V10", "M16,28 H32 V37 H16 Z", "M11,24 H37"),
        tint = Pastel.Lilac,
    )
    val SCHOOL = IconSpec(
        fill = listOf("M6,22 L24,12 L42,22 V41 H6 Z"),
        accent = listOf("M20,30 H28 V41 H20 Z", "M10,26 H16 V31 H10 Z", "M32,26 H38 V31 H32 Z"),
        dot = listOf(),
        line = listOf("M6,22 L24,12 L42,22 V41 H6 Z", "M20,30 H28 V41 H20 Z", "M24,12 V5 L31,7.5 L24,10", "M10,26 H16 V31 H10 Z", "M32,26 H38 V31 H32 Z"),
        tint = Pastel.Pink,
    )
    val RULES = IconSpec(
        fill = listOf("M24,14 C18,9.5 11,9.5 5,12 V38 C11,35.5 18,35.5 24,40 C30,35.5 37,35.5 43,38 V12 C37,9.5 30,9.5 24,14 Z"),
        accent = listOf(),
        dot = listOf(),
        line = listOf("M24,14 C18,9.5 11,9.5 5,12 V38 C11,35.5 18,35.5 24,40 C30,35.5 37,35.5 43,38 V12 C37,9.5 30,9.5 24,14 Z", "M24,14 V40", "M10,30 L14,19 L18,30", "M11.5,26 H16.5", "M29,19 H38", "M29,24.5 H38", "M29,30 H35"),
        tint = Pastel.Blue,
    )
    val STAR = IconSpec(
        fill = listOf("M24.0,6.5 L29.0,18.6 L42.1,19.6 L32.1,28.1 L35.2,40.9 L24.0,34.0 L12.8,40.9 L15.9,28.1 L5.9,19.6 L19.0,18.6 Z"),
        accent = listOf("M17,20 L19.5,18"),
        dot = listOf(),
        line = listOf("M24.0,6.5 L29.0,18.6 L42.1,19.6 L32.1,28.1 L35.2,40.9 L24.0,34.0 L12.8,40.9 L15.9,28.1 L5.9,19.6 L19.0,18.6 Z"),
        tint = Pastel.Yellow,
    )
    val FLAME = IconSpec(
        fill = listOf("M24,5 C31,13 37,18 37,29 A13,13 0 0 1 11,29 C11,22 15,18 18,13 C19,19 22,21 24,22 C26,16 25.5,10 24,5 Z"),
        accent = listOf("M24,41 A6.5,6.5 0 0 1 17.5,34.5 C17.5,30.5 20.5,28 22.5,25 C23.5,28.5 30.5,30 30.5,34.5 A6.5,6.5 0 0 1 24,41 Z"),
        dot = listOf(),
        line = listOf("M24,5 C31,13 37,18 37,29 A13,13 0 0 1 11,29 C11,22 15,18 18,13 C19,19 22,21 24,22 C26,16 25.5,10 24,5 Z"),
        tint = Pastel.Orange,
    )
    val MIC = IconSpec(
        fill = listOf("M17,12 A7,7 0 0 1 31,12 V23 A7,7 0 0 1 17,23 Z"),
        accent = listOf("M20.5,11 V16"),
        dot = listOf(),
        line = listOf("M17,12 A7,7 0 0 1 31,12 V23 A7,7 0 0 1 17,23 Z", "M10.5,22 A13.5,13.5 0 0 0 37.5,22", "M24,35.5 V42", "M17,42 H31"),
        tint = Pastel.Lilac,
    )
    val SPEAKER = IconSpec(
        fill = listOf("M7,18 H15 L26,9 V39 L15,30 H7 Z"),
        accent = listOf(),
        dot = listOf(),
        line = listOf("M7,18 H15 L26,9 V39 L15,30 H7 Z", "M31.5,18.5 A7.5,7.5 0 0 1 31.5,29.5", "M36,13 A15,15 0 0 1 36,35"),
        tint = Pastel.Pink,
    )
    val TURTLE = IconSpec(
        fill = listOf("M7,32 A15,13 0 0 1 37,32 Z", "M36.0,28 a4.5,4.5 0 1,0 9.0,0 a4.5,4.5 0 1,0 -9.0,0 Z"),
        accent = listOf("M14,26 L18,21 H26 L30,26 L26,30 H18 Z"),
        dot = listOf("M40.3,27 a1.2,1.2 0 1,0 2.4,0 a1.2,1.2 0 1,0 -2.4,0 Z"),
        line = listOf("M7,32 A15,13 0 0 1 37,32 Z", "M36.0,28 a4.5,4.5 0 1,0 9.0,0 a4.5,4.5 0 1,0 -9.0,0 Z", "M14,26 L18,21 H26 L30,26 L26,30 H18 Z", "M12,32 V37", "M30,32 V37", "M7,32 L3,34"),
        tint = Pastel.Mint,
    )
    val GEAR = IconSpec(
        fill = listOf("M38.5,24.0 L42.6,27.7 L41.6,31.3 L36.1,32.1 L34.3,34.3 L34.6,39.8 L31.3,41.6 L26.8,38.2 L24.0,38.5 L20.3,42.6 L16.7,41.6 L15.9,36.1 L13.7,34.3 L8.2,34.6 L6.4,31.3 L9.8,26.8 L9.5,24.0 L5.4,20.3 L6.4,16.7 L11.9,15.9 L13.7,13.7 L13.4,8.2 L16.7,6.4 L21.2,9.8 L24.0,9.5 L27.7,5.4 L31.3,6.4 L32.1,11.9 L34.3,13.7 L39.8,13.4 L41.6,16.7 L38.2,21.2 Z"),
        accent = listOf("M18,24 a6,6 0 1,0 12,0 a6,6 0 1,0 -12,0 Z"),
        dot = listOf(),
        line = listOf("M38.5,24.0 L42.6,27.7 L41.6,31.3 L36.1,32.1 L34.3,34.3 L34.6,39.8 L31.3,41.6 L26.8,38.2 L24.0,38.5 L20.3,42.6 L16.7,41.6 L15.9,36.1 L13.7,34.3 L8.2,34.6 L6.4,31.3 L9.8,26.8 L9.5,24.0 L5.4,20.3 L6.4,16.7 L11.9,15.9 L13.7,13.7 L13.4,8.2 L16.7,6.4 L21.2,9.8 L24.0,9.5 L27.7,5.4 L31.3,6.4 L32.1,11.9 L34.3,13.7 L39.8,13.4 L41.6,16.7 L38.2,21.2 Z", "M18,24 a6,6 0 1,0 12,0 a6,6 0 1,0 -12,0 Z"),
        tint = Pastel.Yellow,
    )
    val BACK = IconSpec(
        fill = listOf(),
        accent = listOf(),
        dot = listOf(),
        line = listOf("M29,9 L14,24 L29,39"),
        tint = Pastel.Pink,
    )
    val HOME = IconSpec(
        fill = listOf("M7,22 L24,8 L41,22 V41 H7 Z"),
        accent = listOf("M19,29 H29 V41 H19 Z"),
        dot = listOf(),
        line = listOf("M7,22 L24,8 L41,22 V41 H7 Z", "M19,29 H29 V41 H19 Z", "M4,24.5 L24,8 L44,24.5"),
        tint = Pastel.Lilac,
    )
    val BULB = IconSpec(
        fill = listOf("M24,5 A12.5,12.5 0 0 1 31,28 V32 H17 V28 A12.5,12.5 0 0 1 24,5 Z"),
        accent = listOf("M18,15 A7,7 0 0 1 22,11"),
        dot = listOf(),
        line = listOf("M24,5 A12.5,12.5 0 0 1 31,28 V32 H17 V28 A12.5,12.5 0 0 1 24,5 Z", "M18,37 H30", "M20.5,42 H27.5"),
        tint = Pastel.Yellow,
    )
    val CHECK = IconSpec(
        fill = listOf("M5,24 a19,19 0 1,0 38,0 a19,19 0 1,0 -38,0 Z"),
        accent = listOf(),
        dot = listOf(),
        line = listOf("M5,24 a19,19 0 1,0 38,0 a19,19 0 1,0 -38,0 Z", "M15,24.5 L21.5,31 L33.5,17.5"),
        tint = Pastel.Mint,
    )
    val CROSS = IconSpec(
        fill = listOf("M5,24 a19,19 0 1,0 38,0 a19,19 0 1,0 -38,0 Z"),
        accent = listOf(),
        dot = listOf(),
        line = listOf("M5,24 a19,19 0 1,0 38,0 a19,19 0 1,0 -38,0 Z", "M17,17 L31,31", "M31,17 L17,31"),
        tint = Pastel.Pink,
    )
    val TROPHY = IconSpec(
        fill = listOf("M14,7 H34 V18 A10,10 0 0 1 14,18 Z", "M16,36 H32 V42 H16 Z"),
        accent = listOf("M18,11 V17"),
        dot = listOf(),
        line = listOf("M14,7 H34 V18 A10,10 0 0 1 14,18 Z", "M14,10.5 H9 A5,5 0 0 0 14,20.5", "M34,10.5 H39 A5,5 0 0 1 34,20.5", "M24,28 V36", "M16,36 H32 V42 H16 Z"),
        tint = Pastel.Yellow,
    )
    val GALLERY = IconSpec(
        fill = listOf("M10,9 H38 A5,5 0 0 1 43,14 V34 A5,5 0 0 1 38,39 H10 A5,5 0 0 1 5,34 V14 A5,5 0 0 1 10,9 Z"),
        accent = listOf("M7,35 L17,24 L25,32 L30,27 L41,37 Z"),
        dot = listOf("M29.5,18 a3.5,3.5 0 1,0 7.0,0 a3.5,3.5 0 1,0 -7.0,0 Z"),
        line = listOf("M10,9 H38 A5,5 0 0 1 43,14 V34 A5,5 0 0 1 38,39 H10 A5,5 0 0 1 5,34 V14 A5,5 0 0 1 10,9 Z", "M7,35 L17,24 L25,32 L30,27 L41,37"),
        tint = Pastel.Blue,
    )
    val PENCIL = IconSpec(
        fill = listOf("M31,7 L41,17 L19,39 L8,40 L9,29 Z"),
        accent = listOf("M28,10 L38,20"),
        dot = listOf(),
        line = listOf("M31,7 L41,17 L19,39 L8,40 L9,29 Z", "M27,11 L37,21", "M9,29 L19,39"),
        tint = Pastel.Yellow,
    )
    val SPARKLE = IconSpec(
        fill = listOf("M22,6 C24,18 28,22 40,24 C28,26 24,30 22,42 C20,30 16,26 4,24 C16,22 20,18 22,6 Z", "M38,4 C38.5,8 40,9.5 44,10 C40,10.5 38.5,12 38,16 C37.5,12 36,10.5 32,10 C36,9.5 37.5,8 38,4 Z"),
        accent = listOf(),
        dot = listOf(),
        line = listOf("M22,6 C24,18 28,22 40,24 C28,26 24,30 22,42 C20,30 16,26 4,24 C16,22 20,18 22,6 Z", "M38,4 C38.5,8 40,9.5 44,10 C40,10.5 38.5,12 38,16 C37.5,12 36,10.5 32,10 C36,9.5 37.5,8 38,4 Z"),
        tint = Pastel.Blue,
    )
    val PLAY = IconSpec(
        fill = listOf("M16,9 L39,24 L16,39 Z"),
        accent = listOf(),
        dot = listOf(),
        line = listOf("M16,9 L39,24 L16,39 Z"),
        tint = Pastel.Yellow,
    )
    val SEARCH = IconSpec(
        fill = listOf("M8,21 a13,13 0 1,0 26,0 a13,13 0 1,0 -26,0 Z"),
        accent = listOf("M14,17 A7,7 0 0 1 18,13"),
        dot = listOf(),
        line = listOf("M8,21 a13,13 0 1,0 26,0 a13,13 0 1,0 -26,0 Z", "M30.5,30.5 L42,42"),
        tint = Pastel.Mint,
    )
    val BELL = IconSpec(
        fill = listOf("M24,6 A11,11 0 0 1 35,17 V27 L40,34 H8 L13,27 V17 A11,11 0 0 1 24,6 Z"),
        accent = listOf(),
        dot = listOf(),
        line = listOf("M24,6 A11,11 0 0 1 35,17 V27 L40,34 H8 L13,27 V17 A11,11 0 0 1 24,6 Z", "M19.5,38.5 A5,5 0 0 0 28.5,38.5"),
        tint = Pastel.Lilac,
    )
    val HEART = IconSpec(
        fill = listOf("M24,41 C10,31 5,24 5,17 A9.5,9.5 0 0 1 24,13 A9.5,9.5 0 0 1 43,17 C43,24 38,31 24,41 Z"),
        accent = listOf("M12,16 A5,5 0 0 1 16,12"),
        dot = listOf(),
        line = listOf("M24,41 C10,31 5,24 5,17 A9.5,9.5 0 0 1 24,13 A9.5,9.5 0 0 1 43,17 C43,24 38,31 24,41 Z"),
        tint = Pastel.Pink,
    )
}
