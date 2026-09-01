package ru.plumsoftware.habitstracker.ui.theme

import androidx.compose.ui.unit.dp

object Dimen {
    val small = 4.dp
    val smallPlus = 8.dp
    val medium = 10.dp
    val mediumPlus = 12.dp
    val large = 14.dp
    val extraLarge = 18.dp

    // Внутренние отступы карточек по макету
    val cardPadding = 18.dp

    val bottomBarIconSize = 28.dp

    object Size {
        val defaultCalendarLineItemWidth = 40.dp

        // Квадратный чип с иконкой (настройки, уведомления)
        val iconChip = 44.dp
        // Радиус чекбокса выполнения привычки
        val checkbox = 26.dp
        // Столбец графика активности
        val chartBarWidth = 26.dp
        val chartBarMaxHeight = 90.dp
        // Точка «непрочитано»
        val unreadDot = 8.dp
    }
}
