package ru.plumsoftware.habitstracker.ui.screen.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.database.HabitRepository
import ru.plumsoftware.habitstracker.data.model.Habit
import java.util.Calendar

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
        }
    }

    private fun selectDate(date: Long) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = date
        }
        _state.update {
            it.copy(
                selectedDate = date,
                selectedDay = cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        loadHabitsForDate(date)
    }

    private fun loadHabitsForDate(date: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                withContext(Dispatchers.IO) {

                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadHabits() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                withContext(Dispatchers.IO) {
                    repository.getAllHabits().collect { habits ->
                        _state.update { it.copy(habits = habits, isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun addHabit(habit: Habit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                repository.insertHabit(habit)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                repository.updateHabit(habit)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                repository.deleteHabit(habit)
                if (_state.value.selectedHabit?.id == habit.id) {
                    _state.update { it.copy(selectedHabit = null) }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun selectHabit(habit: Habit?) {
        _state.update { it.copy(selectedHabit = habit) }
    }

    private fun selectDay(day: Int) {
        _state.update {
            it.copy(
                selectedDay = day,
                selectedDate = Calendar.getInstance().apply {
                    timeInMillis = state.value.selectedDate
                    set(Calendar.DAY_OF_MONTH, day)
                }.timeInMillis
            )
        }
    }

    private fun initDate() {
        val currentDate = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentDate
        }
        _state.update {
            it.copy(
                selectedDate = currentDate,
                selectedDay = cal.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    companion object {
        val viewModelModule = module {
            viewModel { HomeViewModel(get()) }
        }
    }
}