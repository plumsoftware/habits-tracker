package ru.plumsoftware.habitstracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Токены цвета из ТЗ §1.1. Ни один цвет не хардкодится в экранах — только через [AppTheme.colors].
 * Светлая и тёмная схемы — [LightHabitColors] / [DarkHabitColors].
 */
@Immutable
data class HabitColors(
    val isDark: Boolean,
    // Акцент
    val accent: Color,
    val accentPressed: Color,
    val accentSoft: Color,
    val accentOnSoft: Color,
    // Тёмные («чернильные») поверхности и текст
    val ink: Color,
    val inkOn: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    // Поверхности
    val bgScreen: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceSunken: Color,
    val surfaceTranslucent: Color,
    val navBarBg: Color,
    val divider: Color,
    val outlineDashed: Color,
    val checkboxOutline: Color,
    val scrim: Color,
    val sheetHandle: Color,
    // Семантика
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val notice: Color,
    val noticeSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val badgeDot: Color,
    // Плитки-«витрины»
    val tileYellow: Color,
    val tileYellowText: Color,
    val tileGreen: Color,
    val tileGreenText: Color,
    val tilePeach: Color,
    val tilePeachText: Color,
    val tileLilac: Color,
    val tileLilacText: Color,
    val tileBlue: Color,
    val tileBlueText: Color,
    val tileMint: Color,
    // Категории (0..3): цвет и soft-фон
    val categoryHealth: Color,
    val categoryHealthSoft: Color,
    val categoryWork: Color,
    val categoryWorkSoft: Color,
    val categoryLearning: Color,
    val categoryLearningSoft: Color,
    val categoryPersonal: Color,
    val categoryPersonalSoft: Color,
    // Тепловая карта (4 уровня)
    val heat0: Color,
    val heat1: Color,
    val heat2: Color,
    val heat3: Color,
) {
    /** Цвет категории по индексу 0..3 (Здоровье, Работа, Обучение, Личное). */
    fun categoryColor(index: Int): Color = when (index) {
        0 -> categoryHealth
        1 -> categoryWork
        2 -> categoryLearning
        else -> categoryPersonal
    }

    fun categorySoft(index: Int): Color = when (index) {
        0 -> categoryHealthSoft
        1 -> categoryWorkSoft
        2 -> categoryLearningSoft
        else -> categoryPersonalSoft
    }

    /** Цвет ячейки heatmap по уровню 0..3. */
    fun heat(level: Int): Color = when (level.coerceIn(0, 3)) {
        0 -> heat0
        1 -> heat1
        2 -> heat2
        else -> heat3
    }
}

val LightHabitColors = HabitColors(
    isDark = false,
    accent = Color(0xFF3A5AFF),
    accentPressed = Color(0xFF2843D6),
    accentSoft = Color(0xFFE4E9FF),
    accentOnSoft = Color(0xFF3A5AFF),
    ink = Color(0xFF0E1220),
    inkOn = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0E1220),
    textSecondary = Color(0xFF7B8299),
    textTertiary = Color(0xFFA2A9BC),
    textDisabled = Color(0xFF9BA2B5),
    bgScreen = Color(0xFFEEF1FA),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF1F4FC),
    surfaceSunken = Color(0xFFF6F8FD),
    surfaceTranslucent = Color(0x99FFFFFF),
    navBarBg = Color(0xEBFFFFFF),
    divider = Color(0xFFF1F4FC),
    outlineDashed = Color(0xFFC3CBEA),
    checkboxOutline = Color(0xFFD8DEEE),
    scrim = Color(0x6B0E1220),
    sheetHandle = Color(0xFFC6CBD9),
    success = Color(0xFF15803D),
    successSoft = Color(0xFFD6F5E3),
    warning = Color(0xFFF59E0B),
    warningSoft = Color(0xFFFFE8A3),
    danger = Color(0xFFE4572E),
    dangerSoft = Color(0xFFFFE3D8),
    notice = Color(0xFF7C3AED),
    noticeSoft = Color(0xFFEBE2FF),
    info = Color(0xFF3A5AFF),
    infoSoft = Color(0xFFD6EEFF),
    badgeDot = Color(0xFFFF5A5A),
    tileYellow = Color(0xFFFFE8A3),
    tileYellowText = Color(0xFF8A6B12),
    tileGreen = Color(0xFFC9F2DC),
    tileGreenText = Color(0xFF2F6B4C),
    tilePeach = Color(0xFFFFD7C2),
    tilePeachText = Color(0xFF8A4A2A),
    tileLilac = Color(0xFFE2D9FF),
    tileLilacText = Color(0xFF4A3F73),
    tileBlue = Color(0xFFD9E3FF),
    tileBlueText = Color(0xFF41508C),
    tileMint = Color(0xFFBFF3D5),
    categoryHealth = Color(0xFF3A5AFF),
    categoryHealthSoft = Color(0xFFE4E9FF),
    categoryWork = Color(0xFFE4572E),
    categoryWorkSoft = Color(0xFFFFE3D8),
    categoryLearning = Color(0xFF7C3AED),
    categoryLearningSoft = Color(0xFFEBE2FF),
    categoryPersonal = Color(0xFF15803D),
    categoryPersonalSoft = Color(0xFFD6F5E3),
    heat0 = Color(0xFFEEF1FA),
    heat1 = Color(0xFFC4D0FF),
    heat2 = Color(0xFF7C93FF),
    heat3 = Color(0xFF3A5AFF),
)

/**
 * Тёмная схема (ТЗ §1.1): bgScreen — самый тёмный слой, surface светлее фона,
 * «чернильные» карточки становятся светлее фона на шаг, акцент подсвечен (#5B77FF)
 * для контраста ≥ 4.5:1, soft-фоны — акцент/семантика с alpha ~18 %.
 */
val DarkHabitColors = HabitColors(
    isDark = true,
    accent = Color(0xFF5B77FF),
    accentPressed = Color(0xFF4763F0),
    accentSoft = Color(0x2E5B77FF),
    accentOnSoft = Color(0xFFA9B9FF),
    ink = Color(0xFF1E2333),
    inkOn = Color(0xFFF3F5FA),
    textPrimary = Color(0xFFF3F5FA),
    textSecondary = Color(0xFF9AA2B6),
    textTertiary = Color(0xFF6E7689),
    textDisabled = Color(0xFF6B7183),
    bgScreen = Color(0xFF0B0E17),
    surface = Color(0xFF161A24),
    surfaceMuted = Color(0xFF1E2330),
    surfaceSunken = Color(0xFF11151E),
    surfaceTranslucent = Color(0x99161A24),
    navBarBg = Color(0xE6161A24),
    divider = Color(0xFF262B38),
    outlineDashed = Color(0xFF39415A),
    checkboxOutline = Color(0xFF363C4C),
    scrim = Color(0x8C05070C),
    sheetHandle = Color(0xFF3A4054),
    success = Color(0xFF34D07A),
    successSoft = Color(0x2E34D07A),
    warning = Color(0xFFFBBF24),
    warningSoft = Color(0x33FBBF24),
    danger = Color(0xFFFF7043),
    dangerSoft = Color(0x33FF7043),
    notice = Color(0xFFA78BFA),
    noticeSoft = Color(0x30A78BFA),
    info = Color(0xFF5B77FF),
    infoSoft = Color(0x2E5B77FF),
    badgeDot = Color(0xFFFF6B6B),
    tileYellow = Color(0xFF3A3320),
    tileYellowText = Color(0xFFE8C766),
    tileGreen = Color(0xFF1E3A2C),
    tileGreenText = Color(0xFF7FE0AD),
    tilePeach = Color(0xFF3A2A20),
    tilePeachText = Color(0xFFE0A583),
    tileLilac = Color(0xFF2A2540),
    tileLilacText = Color(0xFFC3B6F5),
    tileBlue = Color(0xFF22283F),
    tileBlueText = Color(0xFF9DB2F0),
    tileMint = Color(0xFF22402F),
    categoryHealth = Color(0xFF5B77FF),
    categoryHealthSoft = Color(0x2E5B77FF),
    categoryWork = Color(0xFFFF7043),
    categoryWorkSoft = Color(0x33FF7043),
    categoryLearning = Color(0xFFA78BFA),
    categoryLearningSoft = Color(0x30A78BFA),
    categoryPersonal = Color(0xFF34D07A),
    categoryPersonalSoft = Color(0x2E34D07A),
    heat0 = Color(0xFF1A1E29),
    heat1 = Color(0xFF2C3B78),
    heat2 = Color(0xFF4A63C8),
    heat3 = Color(0xFF5B77FF),
)

val LocalHabitColors = staticCompositionLocalOf { LightHabitColors }
