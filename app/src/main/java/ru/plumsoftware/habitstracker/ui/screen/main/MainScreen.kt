package ru.plumsoftware.habitstracker.ui.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.data.model.HabitWithStatus
import ru.plumsoftware.habitstracker.ui.components.AddBottomSheet
import ru.plumsoftware.habitstracker.ui.components.BottomBar
import ru.plumsoftware.habitstracker.ui.components.CalendarView
import ru.plumsoftware.habitstracker.ui.components.HabitCard
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeEvent
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeViewModel
import ru.plumsoftware.habitstracker.ui.screen.main.notifications.NotificationScreen
import ru.plumsoftware.habitstracker.ui.screen.main.settings.SettingsScreen
import ru.plumsoftware.habitstracker.ui.screen.main.settings.SettingsViewModel
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settingsViewModel: SettingsViewModel,
    isDarkTheme: Boolean
) {
    val viewModel: HomeViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.selectedDate,
        yearRange = 2020..2030
    )
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var showBottomSheet by remember { mutableStateOf(false) }

    //Автопрокрутка до ближайшей привычки
    val listState = rememberLazyListState()

    val isEditMode = state.editingHabit != null

    // Находим индекс ближайшей привычки
    val nearestHabitIndex = remember(state.habitsWithStatus) {
        findNearestHabitIndex(state.habitsWithStatus)
    }

    // Автоматическая прокрутка при изменении списка привычек
    LaunchedEffect(state.habitsWithStatus, nearestHabitIndex) {
        if (nearestHabitIndex != -1 && state.habitsWithStatus.isNotEmpty()) {
            // Небольшая задержка для плавности
            delay(300L)
            listState.animateScrollToItem(
                index = nearestHabitIndex,
                scrollOffset = 0
            )
        }
    }

    LaunchedEffect(showBottomSheet) {
        if (!showBottomSheet && state.editingHabit != null) {
            // Если закрыли BottomSheet в режиме редактирования, сбрасываем состояние
            viewModel.onEvent(HomeEvent.CloseEditBottomSheet)
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            viewModel.onEvent(HomeEvent.SelectDate(selectedDate))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = "Ок",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text(
                        text = "Отмена",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        ) {
            DatePicker(
                modifier = Modifier.padding(vertical = Dimen.medium),
                state = datePickerState, // Используем запомненное состояние
                showModeToggle = false,
                title = null
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            when (state.mainScreenState) {
                MainScreenStates.Home -> {}
                MainScreenStates.Notifications -> {}
                MainScreenStates.Settings -> {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "Настройки",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    )
                }

                MainScreenStates.Statistics -> {}
            }
        },
        bottomBar = {
            BottomBar(
                viewModel = viewModel,
                onAdd = {
                    showBottomSheet = true
                }
            )
        }
    ) { innerPadding ->

        when (state.mainScreenState) {
            MainScreenStates.Home -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = Dimen.medium)
                        .padding(top = Dimen.large),
                    verticalArrangement = Arrangement.spacedBy(
                        space = Dimen.medium,
                        alignment = Alignment.Top
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .wrapContentSize(),
                            verticalArrangement = Arrangement.spacedBy(
                                space = Dimen.medium,
                                alignment = Alignment.Top
                            ),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = getMonthTitle(state.selectedDate),
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                text = "Привычки на сегодня: ${state.allHabitsSize}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Card(
                            modifier = Modifier.size(44.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            elevation = CardDefaults.elevatedCardElevation(0.dp, 0.dp, 0.dp),
                            shape = CircleShape,
                            onClick = {
                                showDatePicker = true
                            }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    modifier = Modifier.align(Alignment.Center),
                                    painter = painterResource(R.drawable.calendar_filled),
                                    contentDescription = "Выбрать дату"
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(Dimen.medium)
                    )

                    CalendarView(
                        selectedDate = state.selectedDate,
                        selectedDay = state.selectedDay,
                        onDaySelected = { day ->
                            viewModel.onEvent(HomeEvent.SelectDay(day))
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(Dimen.large)
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(
                            space = Dimen.medium,
                            alignment = Alignment.CenterVertically
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(state.habitsWithStatus) { index, item ->
                            HabitCard(
                                habitWithStatus = item,
                                onClick = { habit ->
                                    viewModel.onEvent(HomeEvent.OpenEditHabit(habit))
                                    showBottomSheet = true
                                },
                                onDone = { isDone ->
                                    if (isDone) {
                                        viewModel.onEvent(HomeEvent.MarkHabitCompleted(item.habit.id))
                                    } else {
                                        viewModel.onEvent(HomeEvent.UnmarkHabitCompleted(item.habit.id))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            MainScreenStates.Notifications -> {
                NotificationScreen()
            }

            MainScreenStates.Settings -> {
                SettingsScreen(
                    padding = innerPadding,
                    settingsViewModel = settingsViewModel,
                    isDark = isDarkTheme
                )
            }

            MainScreenStates.Statistics -> {

            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AddBottomSheet(
                    onSaveHabit = { habit ->
                        viewModel.onEvent(HomeEvent.AddHabit(habit = habit))
                        showBottomSheet = false
                    },
                    onUpdateHabit = { habit ->
                        viewModel.onEvent(HomeEvent.UpdateHabit(habit))
                        showBottomSheet = false
                    },
                    onDeleteHabit = if (isEditMode) {
                        {
                            state.editingHabit?.let { habit ->
                                viewModel.onEvent(HomeEvent.DeleteHabitById(habit.id))
                            }
                            showBottomSheet = false
                        }
                    } else null,
                    editingHabit = state.editingHabit
                )
            }
        }
    }
}

@Composable
fun getMonthTitle(selectedDate: Long): String {
    val cal = Calendar.getInstance().apply {
        timeInMillis = selectedDate
    }
    val monthFormat = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
    return monthFormat.format(cal.time).replaceFirstChar { it.uppercase() }
}

// Функция для поиска индекса ближайшей привычки
private fun findNearestHabitIndex(habits: List<HabitWithStatus>): Int {
    if (habits.isEmpty()) return -1

    val currentTime = Calendar.getInstance()
    val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentTime.get(Calendar.MINUTE)
    val currentTotalMinutes = currentHour * 60 + currentMinute

    var nearestIndex = 0
    var minTimeDifference = Int.MAX_VALUE

    habits.forEachIndexed { index, habitWithStatus ->
        val habitTotalMinutes =
            habitWithStatus.habit.reminderTimeHour * 60 + habitWithStatus.habit.reminderTimeMinute
        var timeDifference = habitTotalMinutes - currentTotalMinutes

        // Если время привычки уже прошло сегодня, учитываем её на завтра
        if (timeDifference < 0) {
            timeDifference += 24 * 60 // Добавляем сутки в минутах
        }

        if (timeDifference < minTimeDifference) {
            minTimeDifference = timeDifference
            nearestIndex = index
        }
    }

    return nearestIndex
}
