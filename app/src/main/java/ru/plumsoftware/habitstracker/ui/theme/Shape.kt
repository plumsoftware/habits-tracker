package ru.plumsoftware.habitstracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val shapes = Shapes(
    // Мелкие элементы: чекбоксы, чипы иконок
    small = RoundedCornerShape(10.dp),
    // Карточки привычек, уведомлений, дни календаря
    medium = RoundedCornerShape(18.dp),
    // Крупные карточки статистики, bottom sheet
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Полностью скруглённая форма (бейдж времени, «таблетки», тумблеры)
val PillShape = RoundedCornerShape(percent = 50)

/**
 * Радиусы и формы из ТЗ §1.2. Доступ — через [AppTheme.shapes].
 */
@Immutable
data class HabitShapes(
    val cardXL: RoundedCornerShape = RoundedCornerShape(30.dp),
    val cardL: RoundedCornerShape = RoundedCornerShape(28.dp),
    val card: RoundedCornerShape = RoundedCornerShape(24.dp),
    val cardS: RoundedCornerShape = RoundedCornerShape(22.dp),
    val sheet: RoundedCornerShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
    val field: RoundedCornerShape = RoundedCornerShape(16.dp),
    val fieldInner: RoundedCornerShape = RoundedCornerShape(18.dp),
    val segment: RoundedCornerShape = RoundedCornerShape(16.dp),
    val segmentActive: RoundedCornerShape = RoundedCornerShape(13.dp),
    val iconTileL: RoundedCornerShape = RoundedCornerShape(22.dp),
    val iconTile: RoundedCornerShape = RoundedCornerShape(17.dp),
    val iconTileS: RoundedCornerShape = RoundedCornerShape(13.dp),
    val chip: RoundedCornerShape = RoundedCornerShape(999.dp),
    val dayCell: RoundedCornerShape = RoundedCornerShape(20.dp),
    val monthCell: RoundedCornerShape = RoundedCornerShape(14.dp),
    val heatCell: RoundedCornerShape = RoundedCornerShape(3.5.dp),
    val navBar: RoundedCornerShape = RoundedCornerShape(28.dp),
    val fab: RoundedCornerShape = RoundedCornerShape(24.dp),
    val button: RoundedCornerShape = RoundedCornerShape(22.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(999.dp),
)

val LocalHabitShapes = staticCompositionLocalOf { HabitShapes() }
