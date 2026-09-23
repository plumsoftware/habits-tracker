package ru.plumsoftware.habitstracker.ui.screen.main.home

/** Вычисленная статистика одной привычки для экрана «Детали» (ТЗ §8). */
data class HabitDetail(
    val habitId: String,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalChecks: Int = 0,
    val successRate: Int = 0, // %
    val missedDays: Int = 0,
    val doneToday: Boolean = false,
    // Тепловая карта: начало дня (ms) -> выполнено (0/1), последние 18 недель
    val heatmap: Map<Long, Int> = emptyMap(),
    // Дни, «спасённые» заморозкой (пропущены, но серия сохранена) — ТЗ §7.4.2
    val frozenDays: Set<Long> = emptySet(),
    // Отметки по дням недели (пн..вс) — 7 значений
    val weekdayCounts: List<Int> = List(7) { 0 },
)
