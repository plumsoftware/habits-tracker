package ru.plumsoftware.habitstracker.ui.screen.main.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.database.HabitRepository
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitWithStatus
import ru.plumsoftware.habitstracker.ui.screen.main.MainScreenStates
import java.util.Calendar
import java.util.Date

class HomeViewModel(
    private val repository: HabitRepository
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    init {
        loadHabits()
        initDate()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadHabits -> loadHabits()
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

    private fun unmarkHabitCompleted(habitId: String) {
        Log.d("HomeViewModel", "Снимаем отметку выполнения с привычки: $habitId")
        viewModelScope.launch {
            try {
                repository.unmarkHabitCompleted(habitId, state.value.selectedDate)
                Log.d("HomeViewModel", "Отметка с привычки $habitId успешно снята")
                // Данные автоматически обновятся через Flow
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

    private fun loadHabitsForDate(date: Long) {
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

                    _state.update {
                        it.copy(
                            habitsWithStatus = sortedHabits,
                            isLoading = false
                        )
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

    // Также обновите loadHabits() аналогичным образом
    private fun loadHabits() {
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
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                // ... обработка ошибок ...
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

    companion object {
        val viewModelModule = module {
            viewModel { HomeViewModel(get()) }
        }
    }
}
