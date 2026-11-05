package ru.plumsoftware.habitstracker.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import ru.plumsoftware.habitstracker.data.database.AppStatsRepository
import ru.plumsoftware.habitstracker.data.database.WellnessRepository
import ru.plumsoftware.habitstracker.data.model.HabitActivityLink
import ru.plumsoftware.habitstracker.data.model.SleepRecord
import ru.plumsoftware.habitstracker.MyApplication
import ru.plumsoftware.habitstracker.ui.ads.AdLoadingDialog
import ru.plumsoftware.habitstracker.ui.ads.AdLoadingState
import ru.plumsoftware.habitstracker.ui.ads.InterstitialAds
import ru.plumsoftware.habitstracker.ui.ads.showAppOpenAd
import ru.plumsoftware.habitstracker.ui.widget.Deeplinks
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.ui.components.HabitNavBar
import ru.plumsoftware.habitstracker.ui.screen.main.today.TodayScreen
import ru.plumsoftware.habitstracker.ui.screen.main.sleep.SleepEntryDialog
import ru.plumsoftware.habitstracker.ui.screen.main.sleep.SleepScreen
import ru.plumsoftware.habitstracker.ui.screen.main.steps.StepCounterScreen
import ru.plumsoftware.habitstracker.ui.screen.main.workouts.WorkoutScreen
import ru.plumsoftware.habitstracker.ui.screen.main.calendar.CalendarScreen
import ru.plumsoftware.habitstracker.ui.screen.main.analytics.AnalyticsScreen
import ru.plumsoftware.habitstracker.ui.screen.templates.TemplatesScreen
import ru.plumsoftware.habitstracker.ui.screen.form.HabitFormScreen
import ru.plumsoftware.habitstracker.ui.screen.detail.HabitDetailScreen
import ru.plumsoftware.habitstracker.ui.components.NotificationsSheet
import androidx.compose.ui.input.nestedscroll.nestedScroll
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeEvent
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeState
import ru.plumsoftware.habitstracker.ui.screen.main.home.HomeViewModel
import ru.plumsoftware.habitstracker.ui.screen.main.notifications.NotificationScreen
import ru.plumsoftware.habitstracker.ui.screen.main.settings.SettingsScreen
import ru.plumsoftware.habitstracker.ui.screen.main.settings.SettingsViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settingsViewModel: SettingsViewModel,
    isDarkTheme: Boolean,
    deeplink: String? = null,
    onDeeplinkHandled: () -> Unit = {},
) {
    val viewModel: HomeViewModel = koinViewModel()
    val tabStates = rememberSaveableStateHolder()
    val state by viewModel.state.collectAsState()
    var showTemplates by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var formHabit by remember { mutableStateOf<ru.plumsoftware.habitstracker.data.model.Habit?>(null) }
    var detailHabit by remember { mutableStateOf<ru.plumsoftware.habitstracker.data.model.Habit?>(null) }
    var showNotifications by remember { mutableStateOf(false) }
    var showStepCounter by remember { mutableStateOf(false) }
    var showWorkouts by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    val wellness: WellnessRepository = koinInject()
    val activityScope = rememberCoroutineScope()
    var sleepPrompt by remember { mutableStateOf<Pair<Long, SleepRecord?>?>(null) }
    var activityActionPending by remember { mutableStateOf(false) }
    var activityError by remember { mutableStateOf<String?>(null) }

    fun refreshAfterActivity() {
        viewModel.refreshWidgets()
        detailHabit?.let { viewModel.onEvent(HomeEvent.LoadHabitDetail(it.id)) }
    }

    fun toggleHabit(habitId: String, done: Boolean, day: Long = state.selectedDate) {
        val link = state.allHabits.firstOrNull { it.id == habitId }?.activityLink
        if (!done || link == null) {
            val event = if (done) HomeEvent.MarkHabitCompleted(habitId) else HomeEvent.UnmarkHabitCompleted(habitId)
            viewModel.onEvent(event)
            return
        }
        if (activityActionPending) return
        activityActionPending = true
        val selectedDay = day.takeIf { it > 0L } ?: System.currentTimeMillis()
        activityScope.launch {
            try {
                when (link) {
                    HabitActivityLink.SLEEP -> sleepPrompt = selectedDay to wellness.sleepForDay(selectedDay).first()
                    HabitActivityLink.WORKOUT -> {
                        if (wellness.completeWorkoutHabitsForDay(selectedDay)) {
                            refreshAfterActivity()
                        } else {
                            val startToday = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            if (selectedDay >= startToday) showWorkouts = true
                            else activityError = "За выбранный день нет завершённой тренировки."
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                activityError = "Не удалось загрузить данные активности. Попробуй ещё раз."
            } finally {
                activityActionPending = false
            }
        }
    }
    val notificationsViewModel: ru.plumsoftware.habitstracker.ui.screen.main.notifications.NotificationsViewModel = koinViewModel()
    val notificationsState by notificationsViewModel.state.collectAsState()

    // §13: нав-бар прячется при скролле вниз, возвращается при скролле вверх
    var navBarVisible by remember { mutableStateOf(true) }
    val navScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (available.y < -3f) navBarVisible = false
                else if (available.y > 3f) navBarVisible = true
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    val activity = LocalActivity.current
    val adLoading = remember { AdLoadingState() }
    val appStats: AppStatsRepository = koinInject()

    val interstitial = MyApplication.platformConfig.adsConfig.interstitial
    var adTransition by remember { mutableStateOf<Job?>(null) }

    fun navigateWithAd(placementId: String, navigate: () -> Unit) {
        if (adTransition?.isActive == true) return
        adTransition = activityScope.launch {
            if (appStats.hasUsedForHours(24)) {
                InterstitialAds.showIfAvailable(activity, placementId, adLoading)
            }
            navigate()
        }
    }

    fun openSleep() {
        navigateWithAd(interstitial.sleepEntry) { showSleep = true }
    }

    fun closeStepCounter() {
        navigateWithAd(interstitial.stepCounterExit) { showStepCounter = false }
    }

    // Deeplink из виджетов (habits://...)
    LaunchedEffect(deeplink, state.allHabits) {
        val link = deeplink ?: return@LaunchedEffect
        when {
            link == Deeplinks.WORKOUTS || link == Deeplinks.STEPS || link == Deeplinks.SLEEP -> {
                showTemplates = false
                showForm = false
                showNotifications = false
                detailHabit = null
                viewModel.onEvent(HomeEvent.ClearHabitDetail)
                showWorkouts = link == Deeplinks.WORKOUTS
                showStepCounter = link == Deeplinks.STEPS
                if (link == Deeplinks.SLEEP) {
                    viewModel.onEvent(HomeEvent.SelectDate(System.currentTimeMillis()))
                    if (!showSleep) openSleep()
                } else {
                    showSleep = false
                }
            }
            link.startsWith("habits://today") -> viewModel.onEvent(HomeEvent.OnHome)
            link.startsWith("habits://analytics") -> viewModel.onEvent(HomeEvent.OnStatistic)
            link.startsWith("habits://templates") -> showTemplates = true
            link.startsWith("habits://habit/new") -> { formHabit = null; showForm = true }
            link.startsWith("habits://habit/") -> {
                val id = link.substringAfterLast('/')
                val habit = state.allHabits.firstOrNull { it.id == id }
                if (habit != null) {
                    detailHabit = habit
                    viewModel.onEvent(HomeEvent.LoadHabitDetail(habit.id))
                } else viewModel.onEvent(HomeEvent.OnHome)
            }
            else -> viewModel.onEvent(HomeEvent.OnHome)
        }
        onDeeplinkHandled()
    }

    LaunchedEffect(state.showOpenAds, activity) {
        if (state.showOpenAds) {
            showAppOpenAd(activity, MyApplication.platformConfig.adsConfig.openAdsId, adLoading)
        }
    }

    LaunchedEffect(viewModel, activity, adLoading) {
        viewModel.habitCreated.collect {
            if (appStats.hasUsedForHours(12)) launch {
                InterstitialAds.showIfAvailable(activity, interstitial.habitCreation, adLoading, cooldownMillis = 65_000L)
            }
        }
    }

    // Child screens register later and handle Back before the tab fallback.
    BackHandler(enabled = state.mainScreenState != MainScreenStates.Home) {
        viewModel.onEvent(HomeEvent.OnHome)
        navBarVisible = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Планшет/большой экран в альбомной ориентации → трёхколоночная вёрстка
        val isExpanded = maxWidth >= 840.dp

        if (isExpanded) {
            TabletShell(
                onOpenStepCounter = { showStepCounter = true },
                onOpenWorkouts = { showWorkouts = true },
                onOpenSleep = ::openSleep,
                state = state,
                settingsViewModel = settingsViewModel,
                isDark = isDarkTheme,
                hasUnread = notificationsState.unreadCount > 0,
                onNav = { target -> viewModel.onEvent(navEventFor(target)) },
                onAdd = { formHabit = null; showForm = true },
                onSelectDate = { millis -> viewModel.onEvent(HomeEvent.SelectDate(millis)) },
                onToggleDone = { habitId, done -> toggleHabit(habitId, done) },
                onHabitClick = { habit ->
                    detailHabit = habit
                    viewModel.onEvent(HomeEvent.LoadHabitDetail(habit.id))
                },
                onOpenNotifications = { showNotifications = true },
                onOpenTemplates = { showTemplates = true },
                onUnarchive = { id -> viewModel.onEvent(HomeEvent.UnarchiveHabit(id)) },
            )
        } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        topBar = {
            when (state.mainScreenState) {
                MainScreenStates.Home -> {}
                MainScreenStates.Calendar -> {}
                MainScreenStates.Notifications -> {}
                MainScreenStates.Settings -> {}

                MainScreenStates.Statistics -> {}
            }
        },
        bottomBar = {
            HabitNavBar(
                current = state.mainScreenState,
                onSelect = { target -> viewModel.onEvent(navEventFor(target)) },
                onAdd = { formHabit = null; showForm = true },
                visible = navBarVisible,
            )
        }
    ) { innerPadding ->

      androidx.compose.foundation.layout.Box(modifier = Modifier.nestedScroll(navScrollConnection)) {
        tabStates.SaveableStateProvider(state.mainScreenState.toString()) {
        when (state.mainScreenState) {
            MainScreenStates.Home -> {
                TodayScreen(
                    state = state,
                    onSelectDate = { millis -> viewModel.onEvent(HomeEvent.SelectDate(millis)) },
                    onToggleDone = { habitId, done -> toggleHabit(habitId, done) },
                    onHabitClick = { habit ->
                        detailHabit = habit
                        viewModel.onEvent(HomeEvent.LoadHabitDetail(habit.id))
                    },
                    onOpenNotifications = { showNotifications = true },
                    onOpenTemplates = { showTemplates = true },
                    hasUnread = notificationsState.unreadCount > 0,
                    onOpenStepCounter = { showStepCounter = true },
                    onOpenWorkouts = { showWorkouts = true },
                    onOpenSleep = ::openSleep,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                )
            }

            MainScreenStates.Calendar -> {
                CalendarScreen(
                    onOpenStepCounter = { showStepCounter = true },
                    onOpenWorkouts = { showWorkouts = true },
                    onOpenSleep = ::openSleep,
                    state = state,
                    onSelectDate = { millis -> viewModel.onEvent(HomeEvent.SelectDate(millis)) },
                    onHabitClick = { habit ->
                        detailHabit = habit
                        viewModel.onEvent(HomeEvent.LoadHabitDetail(habit.id))
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                )
            }

            MainScreenStates.Notifications -> {
                NotificationScreen()
            }

            MainScreenStates.Settings -> {
                SettingsScreen(
                    padding = innerPadding,
                    settingsViewModel = settingsViewModel,
                    isDark = isDarkTheme,
                    overallStreak = state.overallStreak,
                    frozenDays = state.frozenDays,
                    firstDayOfWeek = state.firstDayOfWeek,
                    archivedHabits = state.archivedHabits,
                    onUnarchive = { id -> viewModel.onEvent(HomeEvent.UnarchiveHabit(id)) }
                )
            }

            MainScreenStates.Statistics -> {
                AnalyticsScreen(
                    state = state,
                    onHabitClick = { habit ->
                        detailHabit = habit
                        viewModel.onEvent(HomeEvent.LoadHabitDetail(habit.id))
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                        .consumeWindowInsets(PaddingValues(top = innerPadding.calculateTopPadding()))
                )
            }
        }
        }
      }
            }
        }
    }

    if (showStepCounter) {
        SlideInOnAppear {
            StepCounterScreen(onBack = ::closeStepCounter)
        }
    }

    if (showSleep) {
        SlideInOnAppear {
            SleepScreen(selectedDate = state.selectedDate, onBack = { showSleep = false }, onSaved = ::refreshAfterActivity)
        }
    }

    // §9.1 Лист «Уведомления» — по колоколу на «Сегодня»
    if (showNotifications) {
        NotificationsSheet(onDismiss = { showNotifications = false })
    }

    // §8 Детали привычки — полноэкранный оверлей
    detailHabit?.let { habit ->
      SlideInOnAppear {
        HabitDetailScreen(
            habit = habit,
            detail = state.habitDetail,
            category = state.categories.firstOrNull { it.id == habit.categoryId },
            chainParentTitle = habit.chainedAfterId?.let { pid -> state.allHabits.firstOrNull { it.id == pid }?.title },
            onBack = {
                detailHabit = null
                viewModel.onEvent(HomeEvent.ClearHabitDetail)
            },
            onEdit = {
                formHabit = habit
                detailHabit = null
                viewModel.onEvent(HomeEvent.ClearHabitDetail)
                showForm = true
            },
            onArchive = {
                viewModel.onEvent(HomeEvent.ArchiveHabit(habit.id))
                detailHabit = null
                viewModel.onEvent(HomeEvent.ClearHabitDetail)
            },
            onToggleToday = { done ->
                toggleHabit(habit.id, done, System.currentTimeMillis())
                viewModel.onEvent(HomeEvent.LoadHabitDetail(habit.id))
            },
        )
      }
    }

    if (showWorkouts) {
        SlideInOnAppear {
            WorkoutScreen(
                onBack = { showWorkouts = false },
                onWorkoutFinished = ::refreshAfterActivity,
                showCreationAd = {
                    if (appStats.hasUsedForHours(24)) {
                        InterstitialAds.showIfAvailable(activity, interstitial.workoutCreation, adLoading)
                    }
                },
            )
        }
    }

    sleepPrompt?.let { (day, record) ->
        SleepEntryDialog(day = day, record = record, dateLocked = true,
            onDismiss = { sleepPrompt = null },
            onSaved = ::refreshAfterActivity)
    }

    activityError?.let { message ->
        AlertDialog(
            onDismissRequest = { activityError = null },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { activityError = null }) { Text("Понятно") } },
        )
    }

    // §7 Форма «Новая привычка» / «Настроить привычку» — полноэкранный оверлей
    if (showForm) {
      SlideInOnAppear {
        HabitFormScreen(
            editing = formHabit,
            categories = state.categories,
            allHabits = state.allHabits,
            onBack = { showForm = false },
            onSave = { habit ->
                if (formHabit == null) viewModel.onEvent(HomeEvent.AddHabit(habit))
                else viewModel.onEvent(HomeEvent.UpdateHabit(habit))
                showForm = false
            },
            onDelete = { habit ->
                viewModel.onEvent(HomeEvent.DeleteHabit(habit))
                showForm = false
            },
            onOpenTemplates = { showTemplates = true },
            onCreateCategory = { name, color -> viewModel.onEvent(HomeEvent.CreateCategory(name, color)) },
        )
      }
    }

    // §6 Экран «Шаблоны» — полноэкранный оверлей поверх вкладок
    if (showTemplates) {
      SlideInOnAppear {
        TemplatesScreen(
            onBack = { showTemplates = false },
            onAddHabit = { habit ->
                viewModel.onEvent(HomeEvent.AddHabit(habit = habit))
                showTemplates = false
                showForm = false
                viewModel.onEvent(HomeEvent.OnHome)
            },
        )
      }
    }

    AdLoadingDialog(adLoading)

}

/** §13: push-экран (оверлей) въезжает справа с фейдом при появлении. */
@Composable
private fun SlideInOnAppear(content: @Composable () -> Unit) {
    val visibleState = androidx.compose.runtime.remember {
        androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true }
    }
    androidx.compose.animation.AnimatedVisibility(
        visibleState = visibleState,
        enter = androidx.compose.animation.slideInHorizontally(
            animationSpec = androidx.compose.animation.core.tween(220, easing = ru.plumsoftware.habitstracker.ui.theme.EaseStandard),
        ) { full -> full / 3 } + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)),
        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)),
    ) { content() }
}

/** Событие навигации по разделам для планшетного рельса. */
private fun navEventFor(target: MainScreenStates): HomeEvent = when (target) {
    MainScreenStates.Home -> HomeEvent.OnHome
    MainScreenStates.Calendar -> HomeEvent.OnCalendar
    MainScreenStates.Statistics -> HomeEvent.OnStatistic
    MainScreenStates.Notifications -> HomeEvent.OnNotifications
    MainScreenStates.Settings -> HomeEvent.OnSettings
}
