package ru.plumsoftware.habitstracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
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
