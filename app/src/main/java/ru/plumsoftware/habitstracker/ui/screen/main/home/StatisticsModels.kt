package ru.plumsoftware.habitstracker.ui.screen.main.home

/**
 * Корреляция между двумя привычками: в какой доле дней, когда выполнена [fromTitle],
 * также выполнена [toTitle].
 */
data class HabitCorrelation(
    val fromTitle: String,
    val toTitle: String,
    val rate: Float // 0..1
)

/** Одна ячейка тепловой карты активности. */
data class HeatmapCell(
    val dayStartMs: Long,
    val completedCount: Int
)
