package ru.plumsoftware.habitstracker.ui.screen.main.home

import ru.plumsoftware.habitstracker.data.model.Category
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitWithStatus
import ru.plumsoftware.habitstracker.ui.screen.main.MainScreenStates

data class HomeState(
    val habitsWithStatus: List<HabitWithStatus> = emptyList(),
    val isLoading: Boolean = false,
    val selectedDate: Long = 0L,
    val selectedHabit: Habit? = null,
    val selectedDay: Int = 0,
    val selectedMonth: Int = 0,
    val errorMessage: String? = null,
    val mainScreenState: MainScreenStates = MainScreenStates.Home,
    val allHabitsSize: Int = 0,
    val isEditBottomSheetOpen: Boolean = false,
    val editingHabit: Habit? = null,
    val monthlyStats: MonthlyStats = MonthlyStats(),
    val dailyStats: DailyStats = DailyStats(),
    val weeklyStats: WeeklyStats = WeeklyStats(),
    // Доля выполненных привычек по дням (начало дня в ms -> 0..1) — для полосок в календаре
    val dayProgress: Map<Long, Float> = emptyMap(),
    // Начало недели из настроек (Calendar.MONDAY / Calendar.SUNDAY)
    val firstDayOfWeek: Int = java.util.Calendar.MONDAY,
    val showOpenAds: Boolean = false,
    // Категории (встроенные + пользовательские), отсортированы
    val categories: List<Category> = emptyList(),
    // Текущая серия (стрик) по каждой привычке: habitId -> количество дней подряд
    val habitStreaks: Map<String, Int> = emptyMap(),
    // Общая серия дней активности (подряд дни с ≥1 выполнением) — для достижений
    val overallStreak: Int = 0,
    // Тепловая карта активности: начало дня (ms) -> число выполнений за день
    val heatmap: Map<Long, Int> = emptyMap(),
    // Связи между привычками (закономерности)
    val correlations: List<HabitCorrelation> = emptyList(),
    // Разблокированы ли закономерности (после недели пользования)
    val patternsUnlocked: Boolean = false
)