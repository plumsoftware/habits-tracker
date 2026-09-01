package ru.plumsoftware.habitstracker.ui.screen.main.home

/** Один день в карточке «Активность за неделю». */
data class DayActivity(
    val dayOfMonth: Int,
    val dayName: String,       // «пн», «вт», …
    val completedCount: Int,   // сколько привычек выполнено в этот день
    val isSelected: Boolean    // выбранный день (выделяется акцентом)
)

/** Данные для карточки «Активность за неделю». */
data class WeeklyStats(
    val days: List<DayActivity> = emptyList()
)
