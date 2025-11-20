package ru.plumsoftware.habitstracker.ui.screen.main.home

data class DailyStats(
    val totalHabits: Int = 0,
    val completedHabits: Int = 0,
    val uncompletedHabits: Int = 0,
    val completionRate: Float = 0f
)