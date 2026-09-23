package ru.plumsoftware.habitstracker.ui.screen.main.home

import ru.plumsoftware.habitstracker.data.model.Category
import ru.plumsoftware.habitstracker.data.model.Habit

sealed interface HomeEvent {
    object LoadHabits : HomeEvent
    data class AddHabit(val habit: Habit) : HomeEvent
    data class UpdateHabit(val habit: Habit) : HomeEvent
    data class DeleteHabit(val habit: Habit) : HomeEvent
    data class SelectHabit(val habit: Habit?) : HomeEvent
    data class SelectDay(val day: Int) : HomeEvent
    data class SelectDate(val date: Long) : HomeEvent
    data class MarkHabitCompleted(val habitId: String) : HomeEvent
    data class UnmarkHabitCompleted(val habitId: String) : HomeEvent

    data class LoadHabitDetail(val habitId: String) : HomeEvent
    data object ClearHabitDetail : HomeEvent

    data class ArchiveHabit(val habitId: String) : HomeEvent
    data class UnarchiveHabit(val habitId: String) : HomeEvent

    data object OnHome: HomeEvent
    data object OnCalendar: HomeEvent
    data object OnStatistic: HomeEvent
    data object OnSettings: HomeEvent
    data object OnNotifications: HomeEvent

    data class OpenEditHabit(val habit: Habit) : HomeEvent
    object CloseEditBottomSheet : HomeEvent
    data class DeleteHabitById(val habitId: String) : HomeEvent

    // Категории
    data class CreateCategory(val name: String, val colorArgb: Long) : HomeEvent
    data class UpdateCategory(val category: Category) : HomeEvent
    data class DeleteCategory(val category: Category) : HomeEvent
}