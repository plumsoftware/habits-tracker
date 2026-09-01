package ru.plumsoftware.habitstracker.ui.screen.main.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.BuildConfig
import ru.plumsoftware.habitstracker.data.settings.ThemeMode
import ru.plumsoftware.habitstracker.data.settings.WeekStart
import ru.plumsoftware.habitstracker.ui.components.IOSSwitch
import ru.plumsoftware.habitstracker.ui.theme.Dimen

private val seriesColor = Color(0xFFF5A623)

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    settingsViewModel: SettingsViewModel,
    isDark: Boolean
) {
    val settings by settingsViewModel.settings.collectAsState()

    // Запрос разрешения на показ уведомлений (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* результат не критичен: настройка уже сохранена */ }

    var showWeekStartDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(horizontal = Dimen.medium),
        verticalArrangement = Arrangement.spacedBy(Dimen.extraLarge)
    ) {
        // УВЕДОМЛЕНИЯ
        item {
            SettingsSection(title = "Уведомления") {
                SettingsToggleRow(
                    icon = Icons.Filled.Notifications,
                    title = "Push-уведомления",
                    checked = settings.pushEnabled,
                    onCheckedChange = { enabled ->
                        settingsViewModel.setPushEnabled(enabled)
                        if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                RowDivider()
                SettingsToggleRow(
                    icon = Icons.Filled.VolumeUp,
                    title = "Звук",
                    checked = settings.soundEnabled,
                    onCheckedChange = settingsViewModel::setSoundEnabled
                )
                RowDivider()
                SettingsToggleRow(
                    icon = Icons.Filled.LocalFireDepartment,
                    iconTint = seriesColor,
                    iconContainer = seriesColor.copy(alpha = 0.15f),
                    title = "Оповещать о сериях",
                    checked = settings.seriesEnabled,
                    onCheckedChange = settingsViewModel::setSeriesEnabled
                )
            }
        }

        // ОСНОВНЫЕ
        item {
            SettingsSection(title = "Основные") {
                SettingsValueRow(
                    icon = Icons.Filled.CalendarMonth,
                    title = "Начало недели",
                    value = weekStartTitle(settings.weekStart),
                    onClick = { showWeekStartDialog = true }
                )
                RowDivider()
                SettingsValueRow(
                    icon = Icons.Filled.DarkMode,
                    title = "Оформление",
                    value = themeModeTitle(settings.themeMode),
                    onClick = { showThemeDialog = true }
                )
                RowDivider()
                SettingsValueRow(
                    icon = Icons.Filled.Info,
                    title = "О приложении",
                    value = "",
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }

    if (showWeekStartDialog) {
        ChoiceDialog(
            title = "Начало недели",
            options = WeekStart.entries.map { it to weekStartTitle(it) },
            selected = settings.weekStart,
            onSelect = {
                settingsViewModel.setWeekStart(it)
                showWeekStartDialog = false
            },
            onDismiss = { showWeekStartDialog = false }
        )
    }

    if (showThemeDialog) {
        ChoiceDialog(
            title = "Оформление",
            options = ThemeMode.entries.map { it to themeModeTitle(it) },
            selected = settings.themeMode,
            onSelect = {
                settingsViewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Ок")
                }
            },
            title = { Text("О приложении") },
            text = {
                Text(
                    "Трекер привычек\nВерсия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                )
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimen.smallPlus)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Dimen.mediumPlus)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun IconChip(
    icon: ImageVector,
    tint: Color,
    container: Color
) {
    Box(
        modifier = Modifier
            .size(Dimen.Size.iconChip)
            .background(color = container, shape = MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconContainer: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimen.large, vertical = Dimen.mediumPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimen.mediumPlus)
    ) {
        IconChip(icon = icon, tint = iconTint, container = iconContainer)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IOSSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconContainer: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimen.large, vertical = Dimen.mediumPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimen.mediumPlus)
    ) {
        IconChip(icon = icon, tint = iconTint, container = iconContainer)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = Dimen.large + Dimen.Size.iconChip + Dimen.mediumPlus),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == selected,
                                onClick = { onSelect(value) }
                            )
                            .padding(vertical = Dimen.smallPlus),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimen.smallPlus)
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) }
                        )
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    )
}

private fun weekStartTitle(weekStart: WeekStart): String = when (weekStart) {
    WeekStart.MONDAY -> "Понедельник"
    WeekStart.SUNDAY -> "Воскресенье"
}

private fun themeModeTitle(mode: ThemeMode): String = when (mode) {
    ThemeMode.LIGHT -> "Светлое"
    ThemeMode.DARK -> "Тёмное"
    ThemeMode.SYSTEM -> "Системное"
}
