package ru.plumsoftware.habitstracker.ui.screen.main

import android.app.Activity
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yandex.mobile.ads.appopenad.AppOpenAd
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import org.koin.androidx.compose.koinViewModel
import ru.plumsoftware.habitstracker.MyApplication
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.ui.components.HabitNavBar
import ru.plumsoftware.habitstracker.ui.screen.main.today.TodayScreen
import ru.plumsoftware.habitstracker.ui.screen.main.steps.StepCounterScreen
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
    val state by viewModel.state.collectAsState()
    var showTemplates by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var formHabit by remember { mutableStateOf<ru.plumsoftware.habitstracker.data.model.Habit?>(null) }
    var detailHabit by remember { mutableStateOf<ru.plumsoftware.habitstracker.data.model.Habit?>(null) }
    var showNotifications by remember { mutableStateOf(false) }
    var showStepCounter by remember { mutableStateOf(false) }
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
    val context = LocalContext.current

    // Deeplink из виджетов (habits://...)
    LaunchedEffect(deeplink, state.allHabits) {
        val link = deeplink ?: return@LaunchedEffect
        when {
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

    LaunchedEffect(state.showOpenAds) {
        if (state.showOpenAds) {
            // Показать рекламу здесь
            showOpenAds(
                baseContext = context,
                activity = activity
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Планшет/большой экран в альбомной ориентации → трёхколоночная вёрстка
        val isExpanded = maxWidth >= 840.dp

        if (isExpanded) {
            TabletShell(
                state = state,
                settingsViewModel = settingsViewModel,
                isDark = isDarkTheme,
                hasUnread = notificationsState.unreadCount > 0,
                onNav = { target -> viewModel.onEvent(navEventFor(target)) },
                onAdd = { formHabit = null; showForm = true },
                onSelectDate = { millis -> viewModel.onEvent(HomeEvent.SelectDate(millis)) },
                onToggleDone = { habitId, done ->
                    if (done) viewModel.onEvent(HomeEvent.MarkHabitCompleted(habitId))
                    else viewModel.onEvent(HomeEvent.UnmarkHabitCompleted(habitId))
                },
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
        when (state.mainScreenState) {
            MainScreenStates.Home -> {
                TodayScreen(
                    state = state,
                    onSelectDate = { millis -> viewModel.onEvent(HomeEvent.SelectDate(millis)) },
                    onToggleDone = { habitId, done ->
                        if (done) viewModel.onEvent(HomeEvent.MarkHabitCompleted(habitId))
                        else viewModel.onEvent(HomeEvent.UnmarkHabitCompleted(habitId))
                    },
                    onHabitClick = { habit ->
                        detailHabit = habit
                        viewModel.onEvent(HomeEvent.LoadHabitDetail(habit.id))
                    },
                    onOpenNotifications = { showNotifications = true },
                    onOpenTemplates = { showTemplates = true },
                    hasUnread = notificationsState.unreadCount > 0,
                    onOpenStepCounter = { showStepCounter = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                )
            }

            MainScreenStates.Calendar -> {
                CalendarScreen(
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
                )
            }
        }
      }
            }
        }
    }

    if (showStepCounter) {
        SlideInOnAppear {
            StepCounterScreen(onBack = { showStepCounter = false })
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
                if (done) viewModel.onEvent(HomeEvent.MarkHabitCompleted(habit.id))
                else viewModel.onEvent(HomeEvent.UnmarkHabitCompleted(habit.id))
                viewModel.onEvent(HomeEvent.LoadHabitDetail(habit.id))
            },
        )
      }
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
