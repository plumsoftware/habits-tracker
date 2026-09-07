package ru.plumsoftware.habitstracker.ui.screen.main

import android.app.Activity
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.CalendarMonth
import com.yandex.mobile.ads.appopenad.AppOpenAd
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import ru.plumsoftware.habitstracker.MyApplication
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitWithStatus
import ru.plumsoftware.habitstracker.ui.components.AchievementsCard
import ru.plumsoftware.habitstracker.ui.components.ActivityHeatmapCard
import ru.plumsoftware.habitstracker.ui.components.AddBottomSheet
import ru.plumsoftware.habitstracker.ui.components.BottomBar
import ru.plumsoftware.habitstracker.ui.components.CalendarPickerDialog
import ru.plumsoftware.habitstracker.ui.components.CalendarView
import ru.plumsoftware.habitstracker.ui.components.DailyStatsCard
import ru.plumsoftware.habitstracker.ui.components.HabitCard
import ru.plumsoftware.habitstracker.ui.components.HabitCorrelationsCard
import ru.plumsoftware.habitstracker.ui.components.MonthlyStatsCard
import ru.plumsoftware.habitstracker.ui.components.WeeklyActivityCard
import ru.plumsoftware.habitstracker.ui.components.StatisticsSection
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeEvent
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeState
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeViewModel
import ru.plumsoftware.habitstracker.ui.screen.main.notifications.NotificationScreen
import ru.plumsoftware.habitstracker.ui.screen.main.settings.SettingsScreen
import ru.plumsoftware.habitstracker.ui.screen.main.settings.SettingsViewModel
import ru.plumsoftware.habitstracker.ui.theme.Dimen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

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
    // Состояние сетки привычек (используется и для 1, и для 2 колонок)
    val habitsGridState = rememberLazyGridState()
    // Режим отображения списка привычек: false — одна колонка (строкой), true — две колонки
    var isHabitsGrid by rememberSaveable { mutableStateOf(false) }

    val isEditMode = state.editingHabit != null

    // Находим индекс ближайшей привычки
    val nearestHabitIndex = remember(state.habitsWithStatus) {
        findNearestHabitIndex(state.habitsWithStatus)
    }

    val activity = LocalActivity.current
    val context = LocalContext.current

    // Автопрокрутка к ближайшей привычке — только при смене даты/состава списка,
    // но НЕ при отметке выполнения (иначе список «дёргается»)
    LaunchedEffect(state.selectedDate, nearestHabitIndex, isHabitsGrid) {
        if (nearestHabitIndex != -1 && state.habitsWithStatus.isNotEmpty() && !isHabitsGrid) {
            // Небольшая задержка для плавности; автоскролл только в режиме одной колонки
            delay(300L.milliseconds)
            habitsGridState.animateScrollToItem(
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

    LaunchedEffect(state.showOpenAds) {
        if (state.showOpenAds) {
            // Показать рекламу здесь
            showOpenAds(
                baseContext = context,
                activity = activity
            )
        }
    }

    if (showDatePicker) {
        CalendarPickerDialog(
            initialDateMillis = if (state.selectedDate > 0L) state.selectedDate else System.currentTimeMillis(),
            onConfirm = { millis ->
                viewModel.onEvent(HomeEvent.SelectDate(millis))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            firstDow = state.firstDayOfWeek
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Планшет/большой экран в альбомной ориентации → трёхколоночная вёрстка
        val isExpanded = maxWidth >= 840.dp

        if (isExpanded) {
            ExpandedLayout(
                state = state,
                isDark = isDarkTheme,
                isGrid = isHabitsGrid,
                gridState = habitsGridState,
                statsListState = listState,
                onNavigate = { target -> viewModel.onEvent(navEventFor(target)) },
                onToggleGrid = { isHabitsGrid = !isHabitsGrid },
                onOpenCalendar = { showDatePicker = true },
                onSelectDate = { millis -> viewModel.onEvent(HomeEvent.SelectDate(millis)) },
                onHabitClick = { habit ->
                    viewModel.onEvent(HomeEvent.OpenEditHabit(habit))
                    showBottomSheet = true
                },
                onToggleDone = { habitId, done ->
                    if (done) viewModel.onEvent(HomeEvent.MarkHabitCompleted(habitId))
                    else viewModel.onEvent(HomeEvent.UnmarkHabitCompleted(habitId))
                },
                onAddHabit = { showBottomSheet = true },
                settingsContent = { pad ->
                    SettingsScreen(
                        padding = pad,
                        settingsViewModel = settingsViewModel,
                        isDark = isDarkTheme
                    )
                },
                notificationsContent = { NotificationScreen(embedded = true) }
            )
        } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        topBar = {
            when (state.mainScreenState) {
                MainScreenStates.Home -> {}
                MainScreenStates.Notifications -> {}
                MainScreenStates.Settings -> {
                    TopAppBar(
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
                HomeCenterContent(
                    state = state,
                    gridState = habitsGridState,
                    isGrid = isHabitsGrid,
                    onToggleGrid = { isHabitsGrid = !isHabitsGrid },
                    onOpenCalendar = { showDatePicker = true },
                    onSelectDate = { millis -> viewModel.onEvent(HomeEvent.SelectDate(millis)) },
                    onHabitClick = { habit ->
                        viewModel.onEvent(HomeEvent.OpenEditHabit(habit))
                        showBottomSheet = true
                    },
                    onToggleDone = { habitId, done ->
                        if (done) viewModel.onEvent(HomeEvent.MarkHabitCompleted(habitId))
                        else viewModel.onEvent(HomeEvent.UnmarkHabitCompleted(habitId))
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = Dimen.medium)
                        .padding(top = Dimen.large)
                )
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = Dimen.medium)
                        .padding(top = Dimen.large),
                    verticalArrangement = Arrangement.spacedBy(Dimen.medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Заголовок и кнопка календаря
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.wrapContentSize(),
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
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        modifier = Modifier.align(Alignment.Center),
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentDescription = "Выбрать дату"
                                    )
                                }
                            }
                        }
                    }

                    // Spacer
                    item {
                        Spacer(modifier = Modifier.height(Dimen.medium))
                    }

                    // CalendarView
                    item {
                        CalendarView(
                            selectedDate = state.selectedDate,
                            onDateSelected = { millis ->
                                viewModel.onEvent(HomeEvent.SelectDate(millis))
                            },
                            dayProgress = state.dayProgress,
                            firstDow = state.firstDayOfWeek
                        )
                    }

                    // Spacer
                    item {
                        Spacer(modifier = Modifier.height(Dimen.large))
                    }

                    // Статистика - дневная (привязана к выбранному в календаре дню)
                    item {
                        DailyStatsCard(
                            dailyStats = state.dailyStats,
                            selectedDate = state.selectedDate,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(Dimen.large))
                    }

                    // 1. Тепловая карта активности
                    item {
                        ActivityHeatmapCard(
                            heatmap = state.heatmap,
                            endDate = state.selectedDate,
                            firstDayOfWeek = state.firstDayOfWeek,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(Dimen.large))
                    }

                    // 2. Активность за неделю
                    item {
                        WeeklyActivityCard(
                            weeklyStats = state.weeklyStats,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(Dimen.large))
                    }

                    // 3. Статистика за месяц
                    item {
                        MonthlyStatsCard(
                            monthlyStats = state.monthlyStats,
                            selectedMonth = state.selectedMonth,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(Dimen.large))
                    }

                    // 4. Достижения
                    item {
                        AchievementsCard(
                            overallStreak = state.overallStreak,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(Dimen.large))
                    }

                    // 5. Связи между привычками (закономерности)
                    item {
                        HabitCorrelationsCard(
                            correlations = state.correlations,
                            unlocked = state.patternsUnlocked,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Нижний отступ
                    item {
                        Spacer(modifier = Modifier.height(Dimen.extraLarge))
                    }
                }
            }
        }
            }
        }
    }

    // Лист создания/редактирования привычки — общий для обеих вёрсток
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
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
                editingHabit = state.editingHabit,
                categories = state.categories,
                onCreateCategory = { name, color ->
                    viewModel.onEvent(HomeEvent.CreateCategory(name, color))
                },
                onUpdateCategory = { category ->
                    viewModel.onEvent(HomeEvent.UpdateCategory(category))
                },
                onDeleteCategory = { category ->
                    viewModel.onEvent(HomeEvent.DeleteCategory(category))
                }
            )
        }
    }
}

/** Событие навигации по разделам для планшетного рельса. */
private fun navEventFor(target: MainScreenStates): HomeEvent = when (target) {
    MainScreenStates.Home -> HomeEvent.OnHome
    MainScreenStates.Statistics -> HomeEvent.OnStatistic
    MainScreenStates.Notifications -> HomeEvent.OnNotifications
    MainScreenStates.Settings -> HomeEvent.OnSettings
}

/**
 * Центральный контент главного экрана: заголовок с кнопками (переключение колонок + календарь),
 * недельный календарь и сетка привычек (1 или 2 колонки). Переиспользуется в телефонной
 * и планшетной вёрстке.
 */
@Composable
fun HomeCenterContent(
    state: HomeState,
    gridState: LazyGridState,
    isGrid: Boolean,
    onToggleGrid: () -> Unit,
    onOpenCalendar: () -> Unit,
    onSelectDate: (Long) -> Unit,
    onHabitClick: (Habit) -> Unit,
    onToggleDone: (habitId: String, done: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimen.medium, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.wrapContentSize(),
                verticalArrangement = Arrangement.spacedBy(Dimen.medium, Alignment.Top),
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

            Row(horizontalArrangement = Arrangement.spacedBy(Dimen.smallPlus)) {
                // Переключение отображения списка: 1 колонка / 2 колонки
                RoundIconCard(
                    imageVector = if (isGrid) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                    contentDescription = if (isGrid) "В одну колонку" else "В две колонки",
                    onClick = onToggleGrid
                )
                // Выбор даты
                RoundIconCard(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = "Выбрать дату",
                    onClick = onOpenCalendar
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimen.medium))

        CalendarView(
            selectedDate = state.selectedDate,
            onDateSelected = onSelectDate,
            dayProgress = state.dayProgress,
            firstDow = state.firstDayOfWeek
        )

        Spacer(modifier = Modifier.height(Dimen.large))

        // Привычки отсортированы по времени выполнения — от ранней к поздней
        val sortedHabits = remember(state.habitsWithStatus) {
            state.habitsWithStatus.sortedWith(
                compareBy(
                    { it.habit.reminderTimeHour },
                    { it.habit.reminderTimeMinute }
                )
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isGrid) 2 else 1),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(Dimen.medium),
            verticalArrangement = Arrangement.spacedBy(Dimen.medium)
        ) {
            items(
                sortedHabits,
                key = { item -> item.habit.id }
            ) { item ->
                HabitCard(
                    modifier = Modifier.animateItem(),
                    habitWithStatus = item,
                    category = state.categories.firstOrNull { it.id == item.habit.categoryId },
                    streak = state.habitStreaks[item.habit.id] ?: 0,
                    onClick = onHabitClick,
                    onDone = { done -> onToggleDone(item.habit.id, done) }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(Dimen.extraLarge))
            }
        }
    }
}

/** Круглая карточка-кнопка 44dp в стиле кнопки календаря. */
@Composable
fun RoundIconCard(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(44.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(0.dp, 0.dp, 0.dp),
        shape = CircleShape,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                modifier = Modifier.align(Alignment.Center),
                imageVector = imageVector,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = contentDescription
            )
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

@Composable
fun HabitsAndStatisticsContent(
    state: HomeState,
    listState: LazyListState,
    onToggleCompletion: (String) -> Unit,
    onEditHabit: (Habit) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimen.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок привычек
        item {
            Text(
                text = "Привычки на сегодня",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimen.small)
            )
        }

        // Статистика
        item {
            StatisticsSection(state = state)
        }
    }
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

private fun showOpenAds(baseContext: Context, activity: Activity?) {
    val appOpenLoader = AppOpenAdLoader(baseContext)
    val adRequestConfiguration =
        AdRequestConfiguration.Builder(MyApplication.platformConfig.adsConfig.openAdsId).build()

    val appOpenAdEventListener: AppOpenAdEventListener = object : AppOpenAdEventListener {
        override fun onAdShown() {
        }

        override fun onAdDismissed() {

        }

        override fun onAdFailedToShow(adError: AdError) {

        }

        override fun onAdClicked() {

        }

        override fun onAdImpression(impressionData: ImpressionData?) {
        }
    }

    val appOpenAdLoadListener: AppOpenAdLoadListener = object : AppOpenAdLoadListener {
        override fun onAdFailedToLoad(error: AdRequestError) {
        }

        override fun onAdLoaded(appOpenAd: AppOpenAd) {
            appOpenAd.setAdEventListener(appOpenAdEventListener)

            if (activity != null)
                appOpenAd.show(activity)
        }
    }

    appOpenLoader.setAdLoadListener(appOpenAdLoadListener)
    appOpenLoader.loadAd(adRequestConfiguration)
}
