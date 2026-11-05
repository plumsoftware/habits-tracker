package ru.plumsoftware.habitstracker.ui.screen.main.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ru.plumsoftware.habitstracker.BuildConfig
import ru.plumsoftware.habitstracker.data.export.ExportManager
import ru.plumsoftware.habitstracker.data.settings.AppSettings
import ru.plumsoftware.habitstracker.data.settings.ThemeMode
import ru.plumsoftware.habitstracker.data.settings.WeekStart
import ru.plumsoftware.habitstracker.ui.components.AppIcon
import ru.plumsoftware.habitstracker.ui.components.HabitIcons
import ru.plumsoftware.habitstracker.ui.components.IconTile
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.theme.softShadow
import java.util.Calendar

/**
 * Экран «Настройки» (ТЗ §11): баннер стрика, секции «Уведомления» и «Основные»,
 * кнопка повторного онбординга. Работает с [SettingsViewModel].
 */
@Composable
fun SettingsScreen(
    padding: PaddingValues,
    settingsViewModel: SettingsViewModel,
    isDark: Boolean,
    overallStreak: Int = 0,
    frozenDays: Set<Long> = emptySet(),
    firstDayOfWeek: Int = Calendar.MONDAY,
    archivedHabits: List<ru.plumsoftware.habitstracker.data.model.Habit> = emptyList(),
    onUnarchive: (String) -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val settings by settingsViewModel.settings.collectAsState()
    val freezesUsed = remember(frozenDays, firstDayOfWeek) { countThisWeek(frozenDays, firstDayOfWeek) }
    var sheet by remember { mutableStateOf<SettingsSheet?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bgScreen),
        contentPadding = PaddingValues(
            start = spacing.screenH, end = spacing.screenH,
            top = padding.calculateTopPadding() + spacing.screenTop,
            bottom = spacing.screenBottomNav,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)) {
                SettingsPaneA(settings, settingsViewModel, overallStreak, freezesUsed) { sheet = it }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)) {
                SettingsPaneB(settings, settingsViewModel, archivedHabits) { sheet = it }
            }
        }
    }

    SettingsSheets(sheet, { sheet = it }, settings, settingsViewModel, freezesUsed, archivedHabits, onUnarchive)
}

/** Левая колонка настроек: заголовок, серия, уведомления, разрешения. */
@androidx.compose.runtime.Composable
private fun ColumnScope.SettingsPaneA(
    settings: AppSettings,
    settingsViewModel: SettingsViewModel,
    overallStreak: Int,
    freezesUsed: Int,
    onSheet: (SettingsSheet) -> Unit,
) {
    val colors = AppTheme.colors
    Text("Настройки", style = AppTheme.type.h1, color = colors.textPrimary)
    StreakBanner(streak = overallStreak, freezes = (settings.streakFreezesPerWeek - freezesUsed).coerceAtLeast(0))
    SectionLabel("УВЕДОМЛЕНИЯ")
    SectionCard {
        ToggleRow(HabitIcons.bell, colors.accent, colors.accentSoft, "Push-уведомления", "Напоминания о привычках",
            checked = settings.pushEnabled, onChange = settingsViewModel::setPushEnabled)
        Divider()
        ToggleRow(HabitIcons.volume, colors.info, colors.infoSoft, "Звук", "Короткий сигнал при отметке",
            checked = settings.soundEnabled, enabled = settings.pushEnabled, onChange = settingsViewModel::setSoundEnabled)
        Divider()
        ToggleRow(HabitIcons.flame, colors.warning, colors.warningSoft, "Оповещать о сериях", "Когда серия под угрозой",
            checked = settings.seriesEnabled, enabled = settings.pushEnabled, onChange = settingsViewModel::setSeriesEnabled)
        Divider()
        ToggleRow(HabitIcons.moon, colors.notice, colors.noticeSoft, "Тихие часы",
            "С %02d:00 до %02d:00".format(settings.quietStartHour, settings.quietEndHour),
            checked = settings.quietHoursEnabled, enabled = settings.pushEnabled,
            onRowClick = { onSheet(SettingsSheet.QUIET_HOURS) },
            onChange = settingsViewModel::setQuietHoursEnabled)
    }
    SectionLabel("РАЗРЕШЕНИЯ")
    PermissionsSection()
}

/** Правая колонка настроек: основные пункты и опасные действия. */
@androidx.compose.runtime.Composable
private fun ColumnScope.SettingsPaneB(
    settings: AppSettings,
    settingsViewModel: SettingsViewModel,
    archivedHabits: List<ru.plumsoftware.habitstracker.data.model.Habit>,
    onSheet: (SettingsSheet) -> Unit,
) {
    val colors = AppTheme.colors
    SectionLabel("ОСНОВНЫЕ")
    SectionCard {
        NavRow(HabitIcons.navCalendar, colors.accent, colors.accentSoft, "Начало недели",
            weekStartTitle(settings.weekStart)) { onSheet(SettingsSheet.WEEK_START) }
        Divider()
        NavRow(HabitIcons.palette, colors.notice, colors.noticeSoft, "Оформление",
            themeTitle(settings.themeMode)) { onSheet(SettingsSheet.THEME) }
        Divider()
        NavRow(HabitIcons.snowflake, colors.info, colors.infoSoft, "Заморозки стрика",
            "${settings.streakFreezesPerWeek} в неделю") { onSheet(SettingsSheet.FREEZES) }
        Divider()
        NavRow(HabitIcons.export, colors.success, colors.successSoft, "Экспорт данных",
            "CSV, PDF") { onSheet(SettingsSheet.EXPORT) }
        Divider()
        if (archivedHabits.isNotEmpty()) {
            NavRow(HabitIcons.repeat, colors.accent, colors.accentSoft, "Архив привычек",
                "${archivedHabits.size}") { onSheet(SettingsSheet.ARCHIVE) }
            Divider()
        }
        NavRow(HabitIcons.target, colors.warning, colors.warningSoft, "Цель по дням",
            "${settings.weeklyGoalDays} из 7") { onSheet(SettingsSheet.WEEKLY_GOAL) }
        Divider()
        NavRow(HabitIcons.info, colors.textSecondary, colors.surfaceMuted, "О приложении",
            BuildConfig.VERSION_NAME) { onSheet(SettingsSheet.ABOUT) }
    }
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.accentSoft)
            .clickableNoRipple(onClick = { settingsViewModel.resetOnboarding() }).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) { Text("Показать онбординг снова", style = AppTheme.type.bodyS, color = colors.accent) }
    Text(
        "Удалить все данные", style = AppTheme.type.bodyS, color = colors.danger,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .clickableNoRipple(onClick = { onSheet(SettingsSheet.DELETE_ALL) }).padding(vertical = 14.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

/** Планшетная (альбомная) версия настроек — две колонки. */
@androidx.compose.runtime.Composable
fun SettingsScreenTablet(
    settingsViewModel: SettingsViewModel,
    isDark: Boolean,
    overallStreak: Int = 0,
    frozenDays: Set<Long> = emptySet(),
    firstDayOfWeek: Int = Calendar.MONDAY,
    archivedHabits: List<ru.plumsoftware.habitstracker.data.model.Habit> = emptyList(),
    onUnarchive: (String) -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val settings by settingsViewModel.settings.collectAsState()
    val freezesUsed = remember(frozenDays, firstDayOfWeek) { countThisWeek(frozenDays, firstDayOfWeek) }
    var sheet by remember { mutableStateOf<SettingsSheet?>(null) }

    Row(
        modifier = Modifier.fillMaxSize().background(colors.bgScreen)
            .padding(horizontal = spacing.screenH, vertical = spacing.screenTop),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
        ) {
            SettingsPaneA(settings, settingsViewModel, overallStreak, freezesUsed) { sheet = it }
            Spacer(Modifier.height(spacing.screenBottomPlain))
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
        ) {
            SettingsPaneB(settings, settingsViewModel, archivedHabits) { sheet = it }
            Spacer(Modifier.height(spacing.screenBottomPlain))
        }
    }

    SettingsSheets(sheet, { sheet = it }, settings, settingsViewModel, freezesUsed, archivedHabits, onUnarchive)
}

/** Общий диспетчер bottom-sheet'ов настроек (телефон + планшет). */
@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun SettingsSheets(
    sheet: SettingsSheet?,
    setSheet: (SettingsSheet?) -> Unit,
    settings: AppSettings,
    settingsViewModel: SettingsViewModel,
    freezesUsed: Int,
    archivedHabits: List<ru.plumsoftware.habitstracker.data.model.Habit>,
    onUnarchive: (String) -> Unit,
) {
    val exportManager: ExportManager = koinInject()
    val habitRepository: ru.plumsoftware.habitstracker.data.database.HabitRepository = koinInject()
    val scope = rememberCoroutineScope()
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) scope.launch { runCatching { exportManager.importJson(uri) } } }
    fun rangeFromTo(days: Int?): Pair<Long, Long> {
        val to = System.currentTimeMillis()
        return (if (days == null) 0L else to - days.toLong() * 24 * 60 * 60 * 1000) to to
    }
    when (sheet) {
        SettingsSheet.WEEK_START -> OptionsSheet(
            title = "Начало недели",
            options = listOf("Понедельник", "Воскресенье"),
            selectedIndex = if (settings.weekStart == WeekStart.MONDAY) 0 else 1,
            onSelect = { settingsViewModel.setWeekStart(if (it == 0) WeekStart.MONDAY else WeekStart.SUNDAY); setSheet(null) },
            onDismiss = { setSheet(null) },
        )
        SettingsSheet.THEME -> OptionsSheet(
            title = "Оформление",
            options = listOf("Системное", "Светлое", "Тёмное"),
            selectedIndex = when (settings.themeMode) { ThemeMode.SYSTEM -> 0; ThemeMode.LIGHT -> 1; ThemeMode.DARK -> 2 },
            onSelect = { settingsViewModel.setThemeMode(when (it) { 0 -> ThemeMode.SYSTEM; 1 -> ThemeMode.LIGHT; else -> ThemeMode.DARK }); setSheet(null) },
            onDismiss = { setSheet(null) },
        )
        SettingsSheet.FREEZES -> OptionsSheet(
            title = "Заморозки стрика",
            subtitle = "Заморозка закрывает пропущенный день, чтобы серия не сбросилась.\nИспользовано на этой неделе: $freezesUsed из ${settings.streakFreezesPerWeek}",
            options = listOf("0 в неделю", "1 в неделю", "2 в неделю", "3 в неделю"),
            selectedIndex = settings.streakFreezesPerWeek.coerceIn(0, 3),
            onSelect = { settingsViewModel.setStreakFreezesPerWeek(it); setSheet(null) },
            onDismiss = { setSheet(null) },
        )
        SettingsSheet.WEEKLY_GOAL -> OptionsSheet(
            title = "Цель по дням",
            subtitle = "Сколько дней в неделю считается успешной неделей",
            options = (1..7).map { "$it из 7" },
            selectedIndex = (settings.weeklyGoalDays - 1).coerceIn(0, 6),
            onSelect = { settingsViewModel.setWeeklyGoalDays(it + 1); setSheet(null) },
            onDismiss = { setSheet(null) },
        )
        SettingsSheet.EXPORT -> ExportSheet(
            onDismiss = { setSheet(null) },
            onExportCsv = { days -> setSheet(null); val (f, t) = rangeFromTo(days); scope.launch { runCatching { exportManager.share(exportManager.exportCsv(f, t), "text/csv") } } },
            onExportPdf = { days -> setSheet(null); val (f, t) = rangeFromTo(days); scope.launch { runCatching { exportManager.share(exportManager.exportPdf(f, t), "application/pdf") } } },
            onExportJson = { setSheet(null); scope.launch { runCatching { exportManager.share(exportManager.exportJson(), "application/json") } } },
            onImport = { setSheet(null); importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")) },
        )
        SettingsSheet.QUIET_HOURS -> OptionsSheet(
            title = "Тихие часы",
            subtitle = "В этот интервал уведомления откладываются",
            options = QUIET_PRESETS.map { it.label },
            selectedIndex = QUIET_PRESETS.indexOfFirst { it.start == settings.quietStartHour && it.end == settings.quietEndHour },
            onSelect = { i -> settingsViewModel.setQuietHours(QUIET_PRESETS[i].start, QUIET_PRESETS[i].end); setSheet(null) },
            onDismiss = { setSheet(null) },
        )
        SettingsSheet.DELETE_ALL -> DeleteAllSheet(
            onConfirm = { scope.launch { runCatching { habitRepository.wipeAllHabits() } }; setSheet(null) },
            onDismiss = { setSheet(null) },
        )
        SettingsSheet.ARCHIVE -> ArchiveSheet(
            habits = archivedHabits, onUnarchive = { onUnarchive(it) }, onDismiss = { setSheet(null) },
        )
        SettingsSheet.ABOUT -> AboutSheet(onDismiss = { setSheet(null) })
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchiveSheet(
    habits: List<ru.plumsoftware.habitstracker.data.model.Habit>,
    onUnarchive: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true), containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Text("Архив привычек", style = AppTheme.type.h3, color = colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text("История отметок сохранена. Верните привычку, чтобы снова её отслеживать.",
                style = AppTheme.type.caption, color = colors.textSecondary)
            Spacer(Modifier.height(12.dp))
            if (habits.isEmpty()) {
                Text("Архив пуст", style = AppTheme.type.caption, color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 16.dp))
            } else {
                habits.forEach { h ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp))
                                .background(colors.surfaceMuted),
                            contentAlignment = Alignment.Center,
                        ) { Text(h.emoji.ifBlank { "🎯" }, fontSize = 18.sp) }
                        Text(h.title.trim(), style = AppTheme.type.bodyS, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        Text(
                            "Вернуть",
                            style = AppTheme.type.label, color = colors.accent,
                            modifier = Modifier.clip(AppTheme.shapes.pill).background(colors.accentSoft)
                                .clickableNoRipple(onClick = { onUnarchive(h.id) })
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSheet(
    onDismiss: () -> Unit,
    onExportCsv: (Int?) -> Unit,
    onExportPdf: (Int?) -> Unit,
    onExportJson: () -> Unit,
    onImport: () -> Unit,
) {
    val colors = AppTheme.colors
    val ranges = listOf("Неделя" to 7, "Месяц" to 30, "Год" to 365, "Всё время" to null)
    var rangeIndex by remember { mutableStateOf(2) }
    val days = ranges[rangeIndex].second

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true), containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Text("Экспорт данных", style = AppTheme.type.h3, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            Text("Диапазон для CSV и PDF", style = AppTheme.type.overline, color = colors.textSecondary)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceMuted).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ranges.forEachIndexed { i, (label, _) ->
                    val selected = i == rangeIndex
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                            .background(if (selected) colors.accent else Color.Transparent)
                            .clickableNoRipple(onClick = { rangeIndex = i }).padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(label, style = AppTheme.type.captionS, color = if (selected) colors.inkOn else colors.textSecondary) }
                }
            }
            Spacer(Modifier.height(14.dp))
            ExportRow(HabitIcons.export, "Таблица CSV", "Все отметки за период") { onExportCsv(days) }
            ExportRow(HabitIcons.export, "Отчёт PDF", "Сводка за период") { onExportPdf(days) }
            ExportRow(HabitIcons.repeat, "Резервная копия JSON", "Полные данные для переноса") { onExportJson() }
            ExportRow(HabitIcons.info, "Импорт из JSON", "Заменит текущие данные", danger = true) { onImport() }
        }
    }
}

@Composable
private fun ExportRow(icon: Int, title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickableNoRipple(onClick = onClick).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, if (danger) colors.danger else colors.accent, if (danger) colors.dangerSoft else colors.accentSoft, 38.dp, 17.dp, RoundedCornerShape(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = AppTheme.type.bodyS, color = if (danger) colors.danger else colors.textPrimary)
            Text(subtitle, style = AppTheme.type.caption, color = colors.textSecondary)
        }
        AppIcon(HabitIcons.chevronRight, tint = colors.checkboxOutline, size = 15.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteAllSheet(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    var input by remember { mutableStateOf("") }
    val ok = input.trim().equals("удалить", ignoreCase = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true), containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Text("Удалить все данные", style = AppTheme.type.h3, color = colors.danger)
            Spacer(Modifier.height(6.dp))
            Text("Все привычки и история отметок будут удалены безвозвратно. Введите «удалить» для подтверждения.",
                style = AppTheme.type.caption, color = colors.textSecondary)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceMuted).padding(14.dp),
            ) {
                if (input.isEmpty()) Text("удалить", style = AppTheme.type.bodyS, color = colors.textTertiary)
                androidx.compose.foundation.text.BasicTextField(
                    value = input, onValueChange = { input = it }, singleLine = true,
                    textStyle = AppTheme.type.bodyS.copy(color = colors.textPrimary),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.danger),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(14.dp))
            ru.plumsoftware.habitstracker.ui.components.PrimaryButton(
                text = "Удалить навсегда", onClick = onConfirm, enabled = ok,
                background = colors.danger, contentColor = colors.inkOn,
                shadow = ru.plumsoftware.habitstracker.ui.theme.ShadowSpec.None,
            )
        }
    }
}

private enum class SettingsSheet { WEEK_START, THEME, FREEZES, WEEKLY_GOAL, EXPORT, ABOUT, QUIET_HOURS, DELETE_ALL, ARCHIVE }

private data class QuietPreset(val start: Int, val end: Int) { val label get() = "%02d:00 – %02d:00".format(start, end) }
private val QUIET_PRESETS = listOf(QuietPreset(22, 7), QuietPreset(23, 8), QuietPreset(0, 8), QuietPreset(21, 9))

// ---------- §11.1 Баннер стрика ----------

@Composable
private fun StreakBanner(streak: Int, freezes: Int) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = Modifier.fillMaxWidth()
            .softShadow(AppTheme.elevation.cardRaised, shape)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(colors.surface, colors.tileBlue)))
            .padding(17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.tileYellow),
            contentAlignment = Alignment.Center,
        ) { AppIcon(HabitIcons.flame, tint = colors.tileYellowText, size = 23.dp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Текущая серия", style = AppTheme.type.captionS, color = colors.tileBlueText)
            Text("$streak ${daysWord(streak)} без пропусков", style = AppTheme.type.h5, color = colors.textPrimary)
            Text("Заморозки на эту неделю: $freezes", style = AppTheme.type.caption, color = colors.textSecondary)
        }
    }
}

// ---------- Секции ----------

// ---------- §11 Разрешения ----------

@Composable
private fun PermissionsSection() {
    val colors = AppTheme.colors
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }

    // Обновляем статусы при возврате на экран (после системных настроек)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val notifLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    val notifGranted = remember(refresh) { notificationsGranted(context) }
    val exactGranted = remember(refresh) { exactAlarmsGranted(context) }
    val batteryGranted = remember(refresh) { batteryIgnored(context) }

    SectionCard {
        PermissionRow(
            HabitIcons.bell, colors.accent, colors.accentSoft,
            "Уведомления", "Напоминания о привычках", notifGranted,
        ) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !notifGranted) {
                notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else openAppSettings(context)
        }
        Divider()
        PermissionRow(
            HabitIcons.clock, colors.danger, colors.dangerSoft,
            "Точные будильники", "Напоминание точно в срок", exactGranted,
        ) { requestExactAlarms(context) }
        Divider()
        PermissionRow(
            HabitIcons.moon, colors.notice, colors.noticeSoft,
            "Работа в фоне", "Без отключения системой", batteryGranted,
        ) { requestIgnoreBattery(context) }
    }
}

@Composable
private fun PermissionRow(
    icon: Int, iconTint: Color, tile: Color, title: String, subtitle: String,
    granted: Boolean, onAction: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, iconTint, tile, 38.dp, 17.dp, RoundedCornerShape(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = AppTheme.type.bodyS, color = colors.textPrimary)
            Text(subtitle, style = AppTheme.type.caption, color = colors.textSecondary)
        }
        if (granted) {
            Row(
                modifier = Modifier.clip(AppTheme.shapes.pill).background(colors.successSoft)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                AppIcon(HabitIcons.check, tint = colors.success, size = 13.dp)
                Text("Готово", style = AppTheme.type.label, color = colors.success)
            }
        } else {
            Text(
                "Разрешить", style = AppTheme.type.label, color = colors.inkOn,
                modifier = Modifier.clip(AppTheme.shapes.pill).background(colors.accent)
                    .clickableNoRipple(onClick = onAction).padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

private fun notificationsGranted(ctx: android.content.Context): Boolean =
    androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()

private fun exactAlarmsGranted(ctx: android.content.Context): Boolean {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return true
    val am = ctx.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
    return am.canScheduleExactAlarms()
}

private fun batteryIgnored(ctx: android.content.Context): Boolean {
    val pm = ctx.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

private fun openAppSettings(ctx: android.content.Context) {
    runCatching {
        ctx.startActivity(
            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:${ctx.packageName}"))
        )
    }
}

private fun requestExactAlarms(ctx: android.content.Context) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        runCatching { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }
            .onFailure { openAppSettings(ctx) }
    } else openAppSettings(ctx)
}

private fun requestIgnoreBattery(ctx: android.content.Context) {
    runCatching {
        ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }.onFailure { openAppSettings(ctx) }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = AppTheme.type.overline, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().softShadow(AppTheme.elevation.cardRaised, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp)).background(colors.surface).padding(horizontal = 16.dp, vertical = 6.dp),
        content = content,
    )
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(AppTheme.colors.divider))
}

@Composable
private fun ToggleRow(
    icon: Int, iconTint: Color, tile: Color, title: String, subtitle: String,
    checked: Boolean, enabled: Boolean = true, onRowClick: (() -> Unit)? = null, onChange: (Boolean) -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, iconTint, tile, 38.dp, 17.dp, RoundedCornerShape(13.dp))
        Column(
            modifier = Modifier.weight(1f)
                .then(if (onRowClick != null && enabled) Modifier.clickableNoRipple(onClick = onRowClick) else Modifier),
        ) {
            Text(title, style = AppTheme.type.bodyS, color = colors.textPrimary)
            Text(subtitle, style = AppTheme.type.caption, color = colors.textSecondary)
        }
        IOSToggle(checked = checked && enabled, enabled = enabled) { onChange(it) }
    }
}

@Composable
private fun NavRow(icon: Int, iconTint: Color, tile: Color, title: String, value: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickableNoRipple(onClick = onClick).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, iconTint, tile, 38.dp, 17.dp, RoundedCornerShape(13.dp))
        Text(title, style = AppTheme.type.bodyS, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(value, style = AppTheme.type.label, color = colors.textSecondary)
        Spacer(Modifier.size(6.dp))
        AppIcon(HabitIcons.chevronRight, tint = colors.checkboxOutline, size = 15.dp)
    }
}

@Composable
private fun IOSToggle(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = AppTheme.colors
    val trackColor by animateColorAsState(if (checked) colors.accent else colors.checkboxOutline, label = "track")
    val knobOffset by animateDpAsState(if (checked) 23.dp else 3.dp, label = "knob")
    Box(
        modifier = Modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(AppTheme.shapes.pill)
            .background(trackColor)
            .clickableNoRipple(enabled = enabled, onClick = { onChange(!checked) }),
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .align(Alignment.CenterStart)
                .size(24.dp)
                .softShadow(AppTheme.elevation.subtle, AppTheme.shapes.pill)
                .clip(AppTheme.shapes.pill)
                .background(Color.White)
        )
    }
}

// ---------- Листы ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsSheet(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
) {
    val colors = AppTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true), containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Text(title, style = AppTheme.type.h3, color = colors.textPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = AppTheme.type.caption, color = colors.textSecondary)
            }
            Spacer(Modifier.height(12.dp))
            options.forEachIndexed { i, opt ->
                val selected = i == selectedIndex
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (selected) colors.accentSoft else Color.Transparent)
                        .clickableNoRipple(onClick = { onSelect(i) }).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(opt, style = AppTheme.type.bodyS, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    if (selected) AppIcon(HabitIcons.check, tint = colors.accent, size = 16.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSheet(onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true), containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 26.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("О приложении", style = AppTheme.type.h3, color = colors.textPrimary)
            Text("Трекер привычек · версия ${BuildConfig.VERSION_NAME}", style = AppTheme.type.bodyS, color = colors.textPrimary)
            Text("Отмечайте привычки, следите за сериями и статистикой. Данные хранятся только на устройстве.",
                style = AppTheme.type.caption, color = colors.textSecondary)
        }
    }
}

// ---------- Хелперы ----------

private fun weekStartTitle(w: WeekStart) = if (w == WeekStart.MONDAY) "Понедельник" else "Воскресенье"
private fun themeTitle(t: ThemeMode) = when (t) { ThemeMode.SYSTEM -> "Системное"; ThemeMode.LIGHT -> "Светлое"; ThemeMode.DARK -> "Тёмное" }

/** Сколько замороженных дней попадает в текущую неделю (по firstDayOfWeek). */
private fun countThisWeek(frozenDays: Set<Long>, firstDayOfWeek: Int): Int {
    if (frozenDays.isEmpty()) return 0
    val cal = Calendar.getInstance().apply {
        this.firstDayOfWeek = firstDayOfWeek
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    while (cal.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) cal.add(Calendar.DAY_OF_MONTH, -1)
    val weekStart = cal.timeInMillis
    val weekEnd = weekStart + 7L * 24 * 60 * 60 * 1000
    return frozenDays.count { it in weekStart until weekEnd }
}

private fun daysWord(n: Int): String {
    val mod100 = n % 100; val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "дней"
        mod10 == 1 -> "день"
        mod10 in 2..4 -> "дня"
        else -> "дней"
    }
}
