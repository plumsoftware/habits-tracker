package ru.plumsoftware.habitstracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ru.plumsoftware.habitstracker.R

// Единственное семейство шрифтов приложения — Inter (см. res/font). Без системных подмен.
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

private fun inter(
    size: Double,
    line: Double,
    weight: FontWeight,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

/**
 * Полная шкала текстовых стилей из ТЗ (§1.5). Доступ — через [AppTheme.type].
 * Значения (размер / межстрочный / вес / трекинг) взяты из макета один в один.
 */
@Immutable
data class HabitTypography(
    val displayS: TextStyle = inter(30.0, 34.0, FontWeight.ExtraBold, -0.8),
    val h1: TextStyle = inter(26.0, 30.0, FontWeight.ExtraBold, -0.6),
    val h2: TextStyle = inter(23.0, 27.0, FontWeight.ExtraBold, -0.5),
    val h3: TextStyle = inter(19.0, 24.0, FontWeight.ExtraBold),
    val h4: TextStyle = inter(17.0, 21.0, FontWeight.ExtraBold),
    val h5: TextStyle = inter(15.0, 19.0, FontWeight.ExtraBold),
    val numXL: TextStyle = inter(54.0, 54.0, FontWeight.ExtraBold, -2.0),
    val numL: TextStyle = inter(30.0, 33.0, FontWeight.ExtraBold),
    val numM: TextStyle = inter(19.0, 22.0, FontWeight.ExtraBold),
    val bodyL: TextStyle = inter(15.0, 23.0, FontWeight.Medium),
    val body: TextStyle = inter(14.5, 19.0, FontWeight.SemiBold),
    val bodyS: TextStyle = inter(13.5, 18.0, FontWeight.SemiBold),
    val label: TextStyle = inter(12.5, 16.0, FontWeight.SemiBold),
    val caption: TextStyle = inter(11.5, 16.0, FontWeight.Medium),
    val captionS: TextStyle = inter(11.0, 15.0, FontWeight.SemiBold),
    val overline: TextStyle = inter(11.0, 14.0, FontWeight.Bold, 1.0),
    val navLabel: TextStyle = inter(9.5, 12.0, FontWeight.Bold),
)

val LocalHabitTypography = staticCompositionLocalOf { HabitTypography() }

// Material-шкала (для совместимости с существующими экранами) — тоже на Inter.
val AppTypography = Typography(
    displayLarge = inter(57.0, 64.0, FontWeight.ExtraBold),
    displayMedium = inter(45.0, 52.0, FontWeight.ExtraBold),
    displaySmall = inter(30.0, 34.0, FontWeight.ExtraBold, -0.8),
    headlineLarge = inter(32.0, 36.0, FontWeight.ExtraBold),
    headlineMedium = inter(26.0, 30.0, FontWeight.ExtraBold, -0.6),
    headlineSmall = inter(23.0, 27.0, FontWeight.ExtraBold, -0.5),
    titleLarge = inter(19.0, 24.0, FontWeight.ExtraBold),
    titleMedium = inter(17.0, 21.0, FontWeight.ExtraBold),
    titleSmall = inter(15.0, 19.0, FontWeight.ExtraBold),
    bodyLarge = inter(15.0, 23.0, FontWeight.Medium),
    bodyMedium = inter(14.5, 19.0, FontWeight.SemiBold),
    bodySmall = inter(13.5, 18.0, FontWeight.SemiBold),
    labelLarge = inter(14.0, 18.0, FontWeight.SemiBold),
    labelMedium = inter(12.5, 16.0, FontWeight.SemiBold),
    labelSmall = inter(11.0, 15.0, FontWeight.SemiBold),
)

// Хелпер: зачёркнутый текст выполненной привычки.
fun TextStyle.strikethrough(): TextStyle = copy(textDecoration = TextDecoration.LineThrough)
