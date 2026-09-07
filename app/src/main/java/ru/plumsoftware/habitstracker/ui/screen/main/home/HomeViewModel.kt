package ru.plumsoftware.habitstracker.ui.screen.main.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.database.AppStatsRepository
import ru.plumsoftware.habitstracker.data.database.CategoryRepository
import ru.plumsoftware.habitstracker.data.database.HabitRepository
import ru.plumsoftware.habitstracker.data.database.NotificationRepository
import ru.plumsoftware.habitstracker.data.model.Category
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitWithStatus
import ru.plumsoftware.habitstracker.data.model.NotificationType
import ru.plumsoftware.habitstracker.data.settings.SettingsRepository
import ru.plumsoftware.habitstracker.ui.screen.main.MainScreenStates
import java.util.Calendar
import java.util.Date

class HomeViewModel(
    private val repository: HabitRepository,
    private val appStatsRepository: AppStatsRepository,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    init {
        loadHabits()
        initDate()

        recordAppLaunch()
        observeWeekStart()
        observeCategories()
    }

    // Держим список категорий в состоянии актуальным
    private fun observeCategories() {
        viewModelScope.launch {
            categoryRepository.getAll().collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    // Реагируем на смену «Начала недели» в настройках без перезагрузки экрана
    private fun observeWeekStart() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val firstDayOfWeek = when (settings.weekStart) {
                    ru.plumsoftware.habitstracker.data.settings.WeekStart.SUNDAY -> Calendar.SUNDAY
                    else -> Calendar.MONDAY
                }
                if (_state.value.firstDayOfWeek != firstDayOfWeek) {
                    _state.update { it.copy(firstDayOfWeek = firstDayOfWeek) }
                }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadHabits -> loadHabits(isFirstTime = false)
            is HomeEvent.AddHabit -> addHabit(event.habit)
            is HomeEvent.UpdateHabit -> updateHabit(event.habit)
            is HomeEvent.DeleteHabit -> deleteHabit(event.habit)
            is HomeEvent.SelectHabit -> selectHabit(event.habit)
            is HomeEvent.SelectDay -> selectDay(event.day)
            is HomeEvent.SelectDate -> selectDate(event.date)
            is HomeEvent.MarkHabitCompleted -> markHabitCompleted(event.habitId)
            is HomeEvent.UnmarkHabitCompleted -> unmarkHabitCompleted(event.habitId)
            HomeEvent.OnHome -> onMainScreenStateChange(MainScreenStates.Home)
            HomeEvent.OnNotifications -> onMainScreenStateChange(MainScreenStates.Notifications)
            HomeEvent.OnSettings -> onMainScreenStateChange(MainScreenStates.Settings)
            HomeEvent.OnStatistic -> onMainScreenStateChange(MainScreenStates.Statistics)

            is HomeEvent.OpenEditHabit -> openEditHabit(event.habit)
            HomeEvent.CloseEditBottomSheet -> closeEditBottomSheet()
            is HomeEvent.DeleteHabitById -> deleteHabitById(event.habitId)

            is HomeEvent.CreateCategory -> viewModelScope.launch {
                categoryRepository.create(event.name, event.colorArgb)
            }
            is HomeEvent.UpdateCategory -> viewModelScope.launch {
                categoryRepository.update(event.category)
            }
            is HomeEvent.DeleteCategory -> viewModelScope.launch {
                categoryRepository.delete(event.category)
            }
        }
    }

    private fun openEditHabit(habit: Habit) {
        _state.update {
            it.copy(
                isEditBottomSheetOpen = true,
                editingHabit = habit
            )
        }
    }

    private fun closeEditBottomSheet() {
        _state.update {
            it.copy(
                isEditBottomSheetOpen = false,
                editingHabit = null
            )
        }
    }

    private fun deleteHabitById(habitId: String) {
        Log.d("HomeViewModel", "Удаляем привычку по ID: $habitId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                repository.deleteHabitById(habitId)
                Log.d("HomeViewModel", "Привычка с ID $habitId успешно удалена")
                closeEditBottomSheet()
                loadHabitsForDate(state.value.selectedDate)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Ошибка удаления привычки: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка удаления привычки: ${e.message}"
                    )
                }
            }
        }
    }

    private fun onMainScreenStateChange(mainScreenStates: MainScreenStates) {
        _state.update {
            it.copy(
                mainScreenState = mainScreenStates
            )
        }
    }

    private fun selectDate(date: Long) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = date
        }
        _state.update {
            it.copy(
                selectedDate = date,
                selectedDay = cal.get(Calendar.DAY_OF_MONTH),
                selectedMonth = cal.get(Calendar.MONTH)
            )
        }

        loadHabitsForDate(date)
    }

//    private fun loadHabitsForDate(date: Long) {
//        viewModelScope.launch {
//            _state.update { it.copy(isLoading = true) }
//
//            try {
//                repository.getHabitsWithStatus(date).collect { habitsWithStatus ->
//                    // Логируем количество загруженных привычек
//                    Log.d("HomeViewModel", "Загружено привычек: ${habitsWithStatus.size} для даты: ${Date(date)}")
//                    habitsWithStatus.forEachIndexed { index, habitWithStatus ->
//                        Log.d("HomeViewModel", "Привычка $index: ${habitWithStatus.habit.title}, выполнена: ${habitWithStatus.completed}")
//                    }
//
//                    _state.update {
//                        it.copy(
//                            habitsWithStatus = habitsWithStatus,
//                            isLoading = false
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("HomeViewModel", "Ошибка загрузки привычек: ${e.message}", e)
//                _state.update {
//                    it.copy(
//                        isLoading = false,
//                        errorMessage = "Ошибка загрузки привычек: ${e.message}"
//                    )
//                }
//            }
//        }
//    }
//
//    private fun loadHabits() {
//        viewModelScope.launch {
//            _state.update { it.copy(isLoading = true) }
//
//            try {
//                // Загружаем привычки для текущей выбранной даты
//                repository.getHabitsWithStatus(state.value.selectedDate).collect { habitsWithStatus ->
//                    // Логируем количество загруженных привычек
//                    Log.d("HomeViewModel", "Загружено привычек в loadHabits(): ${habitsWithStatus.size}")
//
//                    _state.update {
//                        it.copy(
//                            habitsWithStatus = habitsWithStatus,
//                            isLoading = false
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("HomeViewModel", "Ошибка загрузки привычек в loadHabits(): ${e.message}", e)
//                _state.update {
//                    it.copy(
//                        isLoading = false,
//                        errorMessage = "Ошибка загрузки привычек: ${e.message}"
//                    )
//                }
//            }
//        }
//    }

    private fun markHabitCompleted(habitId: String) {
        Log.d("HomeViewModel", "Отмечаем привычку как выполненную: $habitId")
        viewModelScope.launch {
            try {
                repository.markHabitCompleted(habitId, state.value.selectedDate)
                Log.d("HomeViewModel", "Привычка $habitId успешно отмечена как выполненная")
                // Данные автоматически обновятся через Flow
                notifyHabitCompleted(habitId)
                refreshWidgets()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Ошибка отметки привычки $habitId: ${e.message}", e)
                _state.update {
                    it.copy(
                        errorMessage = "Ошибка отметки привычки: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Пишет уведомления «привычка выполнена» и «серия N дней» в БД
     * с учётом пользовательских настроек (push / оповещать о сериях).
     */
    private suspend fun notifyHabitCompleted(habitId: String) {
        try {
            val settings = settingsRepository.current()
            val title = state.value.habitsWithStatus
                .firstOrNull { it.habit.id == habitId }?.habit?.title ?: return

            if (settings.pushEnabled) {
                notificationRepository.add(
                    title = "Привычка выполнена",
                    body = "Вы отметили «$title». Так держать!",
                    type = NotificationType.HABIT_DONE
                )
            }

            if (settings.seriesEnabled) {
                val streak = calculateStreak(habitId)
                // Уведомляем о серии каждые 3 дня (3, 6, 9, …)
                if (streak >= 3 && streak % 3 == 0) {
                    notificationRepository.add(
                        title = "Серия $streak ${daysWord(streak)}! 🔥",
                        body = "Вы выполняете «$title» ${streak} ${daysWord(streak)} подряд",
                        type = NotificationType.STREAK
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Ошибка уведомления о выполнении: ${e.message}", e)
        }
    }

    /** Просит систему перерисовать виджеты после изменения данных. */
    private fun refreshWidgets() {
        viewModelScope.launch {
            try {
                val context = org.koin.core.context.GlobalContext.get().get<android.content.Context>()
                ru.plumsoftware.habitstracker.ui.widget.WidgetDataLoader.refreshAll(context)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Не удалось обновить виджеты: ${e.message}", e)
            }
        }
    }

    /** Длина серии подряд выполненных дней, заканчивающейся выбранной датой. */
    private suspend fun calculateStreak(habitId: String): Int {
        val completions = repository.habitCompletionDao.getAllCompletionsForHabit(habitId)
        val completedDays = completions
            .filter { it.completed }
            .map { startOfDayMillis(it.completionDate) }
            .toSet()

        val dayMs = 24 * 60 * 60 * 1000L
        var cursor = startOfDayMillis(state.value.selectedDate)
        var streak = 0
        while (completedDays.contains(cursor)) {
            streak++
            cursor -= dayMs
        }
        return streak
    }

    private fun startOfDayMillis(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun daysWord(n: Int): String {
        val mod100 = n % 100
        val mod10 = n % 10
        return when {
            mod100 in 11..14 -> "дней"
            mod10 == 1 -> "день"
            mod10 in 2..4 -> "дня"
            else -> "дней"
        }
    }

    /** Серия подряд выполненных дней для каждой привычки, заканчивающаяся [endDate]. */
    private suspend fun computeHabitStreaks(
        habits: List<HabitWithStatus>,
        endDate: Long
    ): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val dayMs = 24 * 60 * 60 * 1000L
        val end = startOfDayMillis(endDate)
        for (hs in habits) {
            val completions = repository.habitCompletionDao.getAllCompletionsForHabit(hs.habit.id)
            val days = completions.filter { it.completed }
                .map { startOfDayMillis(it.completionDate) }
                .toSet()
            var cursor = end
            var streak = 0
            while (days.contains(cursor)) {
                streak++
                cursor -= dayMs
            }
            result[hs.habit.id] = streak
        }
        return result
    }

    /** Общая серия дней активности: подряд идущие дни с ≥1 выполнением, заканчивая [endDate]. */
    private suspend fun computeOverallStreak(endDate: Long): Int {
        val dayMs = 24 * 60 * 60 * 1000L
        val end = startOfDayMillis(endDate)
        val start = end - 366 * dayMs
        val completions = repository.habitCompletionDao.getCompletedInRange(start, end + dayMs - 1)
        val activeDays = completions.map { startOfDayMillis(it.completionDate) }.toSet()
        var cursor = end
        var streak = 0
        while (activeDays.contains(cursor)) {
            streak++
            cursor -= dayMs
        }
        return streak
    }

    /** Тепловая карта: число выполнений по дням за последние 20 недель, заканчивая [endDate]. */
    private suspend fun loadHeatmap(endDate: Long): Map<Long, Int> {
        val dayMs = 24 * 60 * 60 * 1000L
        val end = startOfDayMillis(endDate)
        val start = end - (20 * 7 - 1) * dayMs
        val completions = repository.habitCompletionDao.getCompletedInRange(start, end + dayMs - 1)
        return completions.groupingBy { startOfDayMillis(it.completionDate) }.eachCount()
    }

    /** Топ связей между привычками: доля дней выполнения A, в которые также выполнена B. */
    private suspend fun computeCorrelations(): List<HabitCorrelation> {
        val habits = repository.getAllHabits().first()
        if (habits.size < 2) return emptyList()
        val limited = habits.take(8)
        val daysByHabit = mutableMapOf<String, Set<Long>>()
        for (h in limited) {
            val comps = repository.habitCompletionDao.getAllCompletionsForHabit(h.id)
            daysByHabit[h.id] = comps.filter { it.completed }
                .map { startOfDayMillis(it.completionDate) }
                .toSet()
        }
        val result = mutableListOf<HabitCorrelation>()
        for (a in limited) {
            val aDays = daysByHabit[a.id] ?: emptySet()
            if (aDays.size < 3) continue
            for (b in limited) {
                if (a.id == b.id) continue
                val bDays = daysByHabit[b.id] ?: emptySet()
                val both = aDays.count { bDays.contains(it) }
                val rate = both.toFloat() / aDays.size
                if (rate > 0f) {
                    result.add(HabitCorrelation(a.title.trim(), b.title.trim(), rate))
                }
            }
        }
        return result.sortedByDescending { it.rate }.take(3)
    }

    /** Закономерности доступны после недели пользования приложением. */
    private fun isPatternsUnlocked(): Boolean {
        return try {
            val context = org.koin.core.context.GlobalContext.get().get<android.content.Context>()
            val firstInstall = context.packageManager
                .getPackageInfo(context.packageName, 0).firstInstallTime
            val weekMs = 7 * 24 * 60 * 60 * 1000L
            System.currentTimeMillis() - firstInstall >= weekMs
        } catch (e: Exception) {
            false
        }
    }

    private fun unmarkHabitCompleted(habitId: String) {
        Log.d("HomeViewModel", "Снимаем отметку выполнения с привычки: $habitId")
        viewModelScope.launch {
            try {
                repository.unmarkHabitCompleted(habitId, state.value.selectedDate)
                Log.d("HomeViewModel", "Отметка с привычки $habitId успешно снята")
                // Данные автоматически обновятся через Flow
                refreshWidgets()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Ошибка снятия отметки с привычки $habitId: ${e.message}", e)
                _state.update {
                    it.copy(
                        errorMessage = "Ошибка снятия отметки: ${e.message}"
                    )
                }
            }
        }
    }

    private fun addHabit(habit: Habit) {
        Log.d("HomeViewModel", "Добавляем новую привычку: ${habit.title}")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                // Создаем копию привычки с выбранной датой создания
                val habitWithSelectedDate = habit.copy(
                    creationDate = getStartOfDay(state.value.selectedDate)
                )
                repository.insertHabit(habitWithSelectedDate)
                Log.d(
                    "HomeViewModel",
                    "Привычка '${habit.title}' успешно добавлена с датой создания: ${Date(state.value.selectedDate)}"
                )
                // После добавления привычки перезагружаем список для текущей даты
                loadHabitsForDate(state.value.selectedDate)
            } catch (e: Exception) {
                Log.e(
                    "HomeViewModel",
                    "Ошибка добавления привычки '${habit.title}': ${e.message}",
                    e
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка добавления привычки: ${e.message}"
                    )
                }
            }
        }
    }

    private fun updateHabit(habit: Habit) {
        Log.d("HomeViewModel", "Обновляем привычку: ${habit.title}")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                repository.updateHabit(habit)
                Log.d("HomeViewModel", "Привычка '${habit.title}' успешно обновлена")
                // После обновления привычки перезагружаем список для текущей даты
                loadHabitsForDate(state.value.selectedDate)
            } catch (e: Exception) {
                Log.e(
                    "HomeViewModel",
                    "Ошибка обновления привычки '${habit.title}': ${e.message}",
                    e
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка обновления привычки: ${e.message}"
                    )
                }
            }
        }
    }

    private fun deleteHabit(habit: Habit) {
        Log.d("HomeViewModel", "Удаляем привычку: ${habit.title}")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                repository.deleteHabit(habit)
                Log.d("HomeViewModel", "Привычка '${habit.title}' успешно удалена")
                if (_state.value.selectedHabit?.id == habit.id) {
                    _state.update { it.copy(selectedHabit = null) }
                }
                // После удаления привычки перезагружаем список для текущей даты
                loadHabitsForDate(state.value.selectedDate)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Ошибка удаления привычки '${habit.title}': ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка удаления привычки: ${e.message}"
                    )
                }
            }
        }
    }

    private fun selectHabit(habit: Habit?) {
        Log.d("HomeViewModel", "Выбрана привычка: ${habit?.title ?: "null"}")
        _state.update { it.copy(selectedHabit = habit) }
    }

    private fun selectDay(day: Int) {
        Log.d("HomeViewModel", "Выбран день: $day")
        val calendar = Calendar.getInstance().apply {
            timeInMillis = state.value.selectedDate
            set(Calendar.DAY_OF_MONTH, day)
        }

        _state.update {
            it.copy(
                selectedDay = day,
                selectedDate = calendar.timeInMillis
            )
        }

        loadHabitsForDate(calendar.timeInMillis)
    }

    private fun initDate() {
        val currentDate = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentDate
        }
        Log.d("HomeViewModel", "Инициализация даты: ${Date(currentDate)}")
        _state.update {
            it.copy(
                selectedDate = currentDate,
                selectedDay = cal.get(Calendar.DAY_OF_MONTH),
                selectedMonth = cal.get(Calendar.MONTH)
            )
        }

        loadHabitsForDate(currentDate)
    }

    fun clearErrorMessage() {
        Log.d("HomeViewModel", "Очистка сообщения об ошибке")
        _state.update { it.copy(errorMessage = null) }
    }

    private fun sortHabitsByTime(habits: List<HabitWithStatus>): List<HabitWithStatus> {
        return habits.sortedWith(
            compareBy(
                { it.habit.reminderTimeHour },
                { it.habit.reminderTimeMinute }
            ))
    }

    private fun loadHabitsForDate(date: Long, isFirstTime: Boolean = true) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val startOfDay = getStartOfDay(date)
                Log.d("HomeViewModel", "Начинаем загрузку привычек для даты: ${Date(startOfDay)}")

                repository.getHabitsWithStatus(startOfDay).collect { habitsWithStatus ->
                    Log.d(
                        "HomeViewModel",
                        "Получено привычек из репозитория: ${habitsWithStatus.size}"
                    )

                    val sortedHabits = sortHabitsByTime(habitsWithStatus)
                    Log.d("HomeViewModel", "После сортировки: ${sortedHabits.size} привычек")

                    // Загружаем статистику
                    val dailyStats = calculateDailyStats(sortedHabits)
                    val monthlyStats = loadMonthlyStats(date)
                    val weeklyStats = loadWeeklyStats(date)
                    val dayProgress = loadDayProgress(date)
                    val firstDayOfWeek = when (settingsRepository.current().weekStart) {
                        ru.plumsoftware.habitstracker.data.settings.WeekStart.SUNDAY -> Calendar.SUNDAY
                        else -> Calendar.MONDAY
                    }

                    // Серии, тепловая карта и закономерности
                    val habitStreaks = computeHabitStreaks(sortedHabits, date)
                    val overallStreak = computeOverallStreak(date)
                    val heatmap = loadHeatmap(date)
                    val correlations = computeCorrelations()
                    val patternsUnlocked = isPatternsUnlocked()

                    if (isFirstTime) {
                        _state.update {
                            it.copy(
                                habitsWithStatus = sortedHabits,
                                allHabitsSize = sortedHabits.size,
                                isLoading = false,
                                dailyStats = dailyStats,
                                monthlyStats = monthlyStats,
                                weeklyStats = weeklyStats,
                                dayProgress = dayProgress,
                                firstDayOfWeek = firstDayOfWeek,
                                habitStreaks = habitStreaks,
                                overallStreak = overallStreak,
                                heatmap = heatmap,
                                correlations = correlations,
                                patternsUnlocked = patternsUnlocked
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                habitsWithStatus = sortedHabits,
                                isLoading = false,
                                dailyStats = dailyStats,
                                monthlyStats = monthlyStats,
                                weeklyStats = weeklyStats,
                                dayProgress = dayProgress,
                                firstDayOfWeek = firstDayOfWeek,
                                habitStreaks = habitStreaks,
                                overallStreak = overallStreak,
                                heatmap = heatmap,
                                correlations = correlations,
                                patternsUnlocked = patternsUnlocked
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Ошибка загрузки привычек: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка загрузки привычек: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun loadMonthlyStats(date: Long): MonthlyStats {
        return try {
            val allHabits = repository.getAllHabits().first()
            val totalHabits = allHabits.size

            // Считаем общее количество выполненных привычек за месяц
            val calendar = Calendar.getInstance().apply { timeInMillis = date }
            val startOfMonth = getStartOfMonth(calendar)
            val endOfMonth = getEndOfMonth(calendar)

            var totalCompletions = 0
            var habitsWithCompletions = 0

            for (habit in allHabits) {
                val completions = repository.habitCompletionDao.getCompletionsInPeriod(
                    habit.id,
                    startOfMonth,
                    endOfMonth
                )
                val completedCount = completions.count { it.completed }
                totalCompletions += completedCount
                if (completedCount > 0) {
                    habitsWithCompletions++
                }
            }

            // Вычисляем невыполненные привычки
            val uncompletedHabits = totalHabits - habitsWithCompletions

            // completionRate = количество привычек, которые были выполнены хоть раз / общее количество привычек
            val completionRate = if (totalHabits > 0) {
                habitsWithCompletions.toFloat() / totalHabits
            } else 0f

            MonthlyStats(
                totalHabits = totalHabits,
                completedHabits = habitsWithCompletions, // Привычки, которые были выполнены хоть раз
                uncompletedHabits = uncompletedHabits,   // Привычки, которые не были выполнены ни разу
                completionRate = completionRate
            )
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Ошибка загрузки месячной статистики: ${e.message}", e)
            MonthlyStats()
        }
    }

    /**
     * Считает активность за неделю, содержащую [date]: для каждого из 7 дней —
     * число выполненных привычек. Начало недели берётся из настроек.
     */
    private suspend fun loadWeeklyStats(date: Long): WeeklyStats {
        return try {
            val firstDayOfWeek = when (settingsRepository.current().weekStart) {
                ru.plumsoftware.habitstracker.data.settings.WeekStart.SUNDAY -> Calendar.SUNDAY
                else -> Calendar.MONDAY
            }

            val dayMs = 24 * 60 * 60 * 1000L
            val selectedStart = startOfDayMillis(date)

            // Находим начало недели
            val cal = Calendar.getInstance().apply {
                this.firstDayOfWeek = firstDayOfWeek
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val delta = (cal.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
            cal.add(Calendar.DAY_OF_MONTH, -delta)
            val weekStartMs = cal.timeInMillis
            val weekEndMs = weekStartMs + 7 * dayMs - 1

            // Считаем выполнения за неделю и раскладываем по дням
            val completions = repository.habitCompletionDao.getCompletedInRange(weekStartMs, weekEndMs)
            val perDay = completions.groupingBy { startOfDayMillis(it.completionDate) }.eachCount()

            val dayNameFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
            val days = (0 until 7).map { i ->
                val dayMillis = weekStartMs + i * dayMs
                val dayCal = Calendar.getInstance().apply { timeInMillis = dayMillis }
                DayActivity(
                    dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH),
                    dayName = dayNameFormat.format(dayCal.time).lowercase(),
                    completedCount = perDay[dayMillis] ?: 0,
                    isSelected = dayMillis == selectedStart
                )
            }
            WeeklyStats(days = days)
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Ошибка загрузки недельной статистики: ${e.message}", e)
            WeeklyStats()
        }
    }

    /**
     * Доля выполненных привычек по дням для месяца вокруг [date]
     * (± неделя, чтобы покрыть крайние ячейки календаря). Ключ — начало дня в ms.
     */
    private suspend fun loadDayProgress(date: Long): Map<Long, Float> {
        return try {
            val total = repository.getAllHabits().first().size
            if (total == 0) return emptyMap()

            val dayMs = 24 * 60 * 60 * 1000L
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            val monthStart = getStartOfMonth(cal) - 7 * dayMs
            val monthEnd = getEndOfMonth(cal) + 7 * dayMs

            val completions = repository.habitCompletionDao.getCompletedInRange(monthStart, monthEnd)
            completions
                .groupingBy { startOfDayMillis(it.completionDate) }
                .eachCount()
                .mapValues { (_, count) -> (count.toFloat() / total).coerceIn(0f, 1f) }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Ошибка расчёта прогресса по дням: ${e.message}", e)
            emptyMap()
        }
    }

    private fun calculateDailyStats(habitsWithStatus: List<HabitWithStatus>): DailyStats {
        val completedHabits = habitsWithStatus.count { it.completed == true }
        val totalHabits = habitsWithStatus.size
        val uncompletedHabits = totalHabits - completedHabits
        val completionRate = if (totalHabits > 0) completedHabits.toFloat() / totalHabits else 0f

        return DailyStats(
            totalHabits = totalHabits,
            completedHabits = completedHabits,
            uncompletedHabits = uncompletedHabits,
            completionRate = completionRate
        )
    }

    private suspend fun calculateDailyCompletions(
        monthlyCompletions: Map<String, Int>,
        calendar: Calendar
    ): Map<Int, Int> {
        val dailyCompletions = mutableMapOf<Int, Int>()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Инициализируем все дни месяца нулями
        for (day in 1..daysInMonth) {
            dailyCompletions[day] = 0
        }

        // Для простоты считаем общее количество выполненных привычек по дням
        // В реальном приложении нужно получать данные по каждому дню из базы
        monthlyCompletions.forEach { (habitId, completions) ->
            // Здесь нужно получить распределение по дням для каждой привычки
            // Для демонстрации равномерно распределяем выполнения
            val completionsPerDay = completions / daysInMonth
            for (day in 1..daysInMonth) {
                dailyCompletions[day] = dailyCompletions[day]!! + completionsPerDay
            }
        }

        return dailyCompletions
    }

    private fun getStartOfMonth(calendar: Calendar): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfMonth(calendar: Calendar): Long {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun getDaysInMonth(calendar: Calendar): Int {
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Также обновите loadHabits() аналогичным образом
    private fun loadHabits(isFirstTime: Boolean = true) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                repository.getHabitsWithStatus(state.value.selectedDate)
                    .collect { habitsWithStatus ->
                        // Сортируем привычки по времени
                        val sortedHabits = sortHabitsByTime(habitsWithStatus)

                        Log.d(
                            "HomeViewModel",
                            "Загружено привычек в loadHabits(): ${sortedHabits.size}"
                        )

                        _state.update {
                            it.copy(
                                habitsWithStatus = sortedHabits,
                                isLoading = false,
                            )
                        }

                        if (isFirstTime) {
                            _state.update {
                                it.copy(
                                    allHabitsSize = sortedHabits.size
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
            }
        }
    }

    private fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    //    region::Launch stats
    private fun recordAppLaunch() {
        viewModelScope.launch {
            val shouldShowAds = appStatsRepository.recordAppLaunch()
            _state.update {
                it.copy(showOpenAds = shouldShowAds)
            }
        }
    }
//    endregion

    companion object {
        val viewModelModule = module {
            viewModel {
                HomeViewModel(
                    get(),
                    get(),
                    get(),
                    get(),
                    get()
                )
            }
        }
    }
}
