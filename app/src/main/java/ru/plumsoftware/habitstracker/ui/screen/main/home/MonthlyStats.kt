package ru.plumsoftware.habitstracker.ui.screen.main.home

data class MonthlyStats(
    val totalHabits: Int = 0,
    val completedHabits: Int = 0,
    val completionRate: Float = 0f,
    val dailyCompletions: Map<Int, Int> = emptyMap() // день месяца -> количество выполненных привычек
)