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
    // Все активные привычки (для фильтра в календаре и т.п.)
    val allHabits: List<Habit> = emptyList(),
    // Архивные привычки (ТЗ §11.3)
    val archivedHabits: List<Habit> = emptyList(),
    // Детали выбранной привычки (экран §8), null = экран закрыт
    val habitDetail: HabitDetail? = null,
    // Текущая серия (стрик) по каждой привычке: habitId -> количество дней подряд
    val habitStreaks: Map<String, Int> = emptyMap(),
    // Общая серия дней активности (подряд дни с ≥1 выполнением) — для достижений
    val overallStreak: Int = 0,
    // Тепловая карта активности: начало дня (ms) -> число выполнений за день
    val heatmap: Map<Long, Int> = emptyMap(),
    // Дни, «спасённые» заморозкой (по всем привычкам) — для календаря и счётчика (§7.4.2)
    val frozenDays: Set<Long> = emptySet(),
    // Дни выполнения по каждой привычке (habitId -> набор startOfDay) — для бинарного календаря §5.2
    val habitCompletionDays: Map<String, Set<Long>> = emptyMap(),
    // Замороженные дни по каждой привычке (habitId -> набор startOfDay)
    val frozenByHabit: Map<String, Set<Long>> = emptyMap(),
    // Достижения (ТЗ §10.6)
    val achievements: List<Achievement> = emptyList(),
    // Связи между привычками (закономерности)
    val correlations: List<HabitCorrelation> = emptyList(),
    // Разблокированы ли закономерности (после недели пользования)
    val patternsUnlocked: Boolean = false
)