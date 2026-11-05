package ru.plumsoftware.habitstracker.ui.screen.main.sleep

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.data.database.WellnessRepository
import ru.plumsoftware.habitstracker.data.model.SleepRecord
import ru.plumsoftware.habitstracker.ui.components.AppScreenTopBar
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val sleepGoalMinutes = 8 * 60

private fun calendarAt(time: Long) = Calendar.getInstance().apply { timeInMillis = time }
private fun midnight(time: Long) = calendarAt(time).apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
private fun offsetDay(day: Long, offset: Int) = calendarAt(day).apply { add(Calendar.DAY_OF_YEAR, offset) }.timeInMillis
private fun atTime(day: Long, minutes: Int) = calendarAt(day).apply {
    set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
private fun timeOfDay(time: Long) = calendarAt(time).run { get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE) }
private fun sleepDate(time: Long, pattern: String) = SimpleDateFormat(pattern, Locale("ru")).format(Date(time))
private fun clockText(minutes: Int) = String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)
private fun durationText(minutes: Int) = if (minutes < 60) "$minutes мин" else "${minutes / 60} ч ${minutes % 60} мин"

@Composable
fun SleepScreen(selectedDate: Long, onBack: () -> Unit, onSaved: () -> Unit = {}) {
    BackHandler(onBack = onBack)
    val repository: WellnessRepository = koinInject()
    val records by remember(repository) { repository.sleepRecords() }.collectAsState(initial = emptyList())
    val colors = AppTheme.colors
    val today = midnight(System.currentTimeMillis())
    val selectedDay = midnight(selectedDate.takeIf { it > 0L } ?: today).coerceAtMost(today)
    val days = remember(selectedDay) { (6 downTo 0).map { offsetDay(selectedDay, -it) } }
    val byDay = remember(records) { records.associateBy { it.dayStartMillis } }
    val week = days.map { byDay[it] }
    val logged = week.filterNotNull()
    val heroRecord = byDay[selectedDay] ?: records.firstOrNull().takeIf { selectedDay == today }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingDay by rememberSaveable { mutableStateOf<Long?>(null) }

    fun openEditor(record: SleepRecord?) {
        editingDay = record?.dayStartMillis
        editorOpen = true
    }

    Column(Modifier.fillMaxSize().background(colors.bgScreen).windowInsetsPadding(WindowInsets.safeDrawing)) {
        AppScreenTopBar("Сон", onBack = onBack)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Box(Modifier.fillMaxWidth().heightIn(min = 116.dp)
                    .clip(RoundedCornerShape(18.dp)).background(colors.accent)) {
                    Image(painterResource(R.drawable.sleep_history_banner), null,
                        Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
                    Column(Modifier.fillMaxWidth(0.63f).align(Alignment.CenterStart)
                        .padding(start = 24.dp, top = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (heroRecord?.dayStartMillis == today) "Прошлая ночь" else
                            sleepDate(heroRecord?.dayStartMillis ?: selectedDay, "d MMMM"),
                            color = Color.White, style = AppTheme.type.bodyS)
                        Text(heroRecord?.let { durationText(it.durationMinutes) } ?: "–",
                            color = Color.White, style = AppTheme.type.h1)
                        Text(heroRecord?.let { "Цель: 8 часов • ${(it.durationMinutes * 100 + sleepGoalMinutes / 2) / sleepGoalMinutes}%" }
                            ?: "Добавь время сна и пробуждения", color = Color.White, style = AppTheme.type.caption)
                    }
                }
            }
            item { SleepWeekChart(days, week, selectedDay) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SleepMetric(Icons.Outlined.BarChart, "В среднем", if (logged.isEmpty()) "–" else durationText(logged.sumOf { it.durationMinutes } / logged.size), Modifier.weight(1f))
                    SleepMetric(Icons.Outlined.DateRange, "Записей", logged.size.toString(), Modifier.weight(1f))
                }
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(colors.surface).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("Последние записи", style = AppTheme.type.h4, color = colors.textPrimary)
                    if (records.isEmpty()) Text("Сохрани первую ночь, чтобы увидеть историю",
                        modifier = Modifier.padding(top = 8.dp), style = AppTheme.type.bodyS, color = colors.textSecondary)
                    records.take(2).forEachIndexed { index, record ->
                        if (index > 0) HorizontalDivider(color = colors.surfaceMuted)
                        SleepRecordRow(record, today) { openEditor(record) }
                    }
                }
            }
            items(records.drop(2), key = { it.dayStartMillis }) { record ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(colors.surface).padding(horizontal = 14.dp)) {
                    SleepRecordRow(record, today) { openEditor(record) }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Button(onClick = { openEditor(null) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White)) {
                Text("＋ Добавить запись", style = AppTheme.type.h5)
            }
        }
    }

    if (editorOpen && (editingDay == null || byDay.containsKey(editingDay))) {
        SleepEntryDialog(
            day = selectedDay,
            record = editingDay?.let { byDay[it] },
            onDismiss = { editorOpen = false },
            onSaved = onSaved,
        )
    }
}

@Composable
internal fun SleepEntryDialog(
    day: Long,
    record: SleepRecord?,
    onDismiss: () -> Unit,
    dateLocked: Boolean = false,
    onSaved: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository: WellnessRepository = koinInject()
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors
    val today = midnight(System.currentTimeMillis())
    var wakeDay by rememberSaveable(day, record?.dayStartMillis) {
        mutableLongStateOf(record?.dayStartMillis ?: midnight(day).coerceAtMost(today))
    }
    var wakeTime by rememberSaveable {
        mutableIntStateOf(record?.let { timeOfDay(it.endedAtMillis) }
            ?: if (wakeDay == today) minOf(7 * 60, timeOfDay(System.currentTimeMillis())) else 7 * 60)
    }
    var hours by rememberSaveable { mutableStateOf(record?.let { (it.durationMinutes / 60).toString() } ?: "") }
    var minutes by rememberSaveable { mutableStateOf(record?.let { (it.durationMinutes % 60).toString() } ?: "") }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val hourCount = hours.toIntOrNull() ?: 0
    val minuteCount = minutes.toIntOrNull() ?: 0
    val duration = hourCount * 60 + minuteCount
    val end = record?.takeIf {
        it.dayStartMillis == wakeDay && timeOfDay(it.endedAtMillis) == wakeTime
    }?.endedAtMillis ?: atTime(wakeDay, wakeTime)
    val start = end - duration * 60_000L
    val validDuration = minuteCount in 0..59 && duration in 1..1440
    val problem = when {
        minuteCount > 59 -> "В поле минут укажи число от 0 до 59"
        duration > 1440 -> "Длительность должна быть не больше 24 часов"
        end > System.currentTimeMillis() -> "Время пробуждения ещё не наступило"
        else -> null
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        containerColor = colors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.sleep_crescent), null, Modifier.size(38.dp))
                Text(if (record == null) "Записать сон" else "Изменить сон", style = AppTheme.type.h3, color = colors.textPrimary)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Сколько ты спал?", style = AppTheme.type.bodyS, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(hours, { hours = it.filter { c -> c in '0'..'9' }.take(2); saveError = null },
                        modifier = Modifier.weight(1f), label = { Text("Часы") }, placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !saving, singleLine = true, shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(minutes, { minutes = it.filter { c -> c in '0'..'9' }.take(2); saveError = null },
                        modifier = Modifier.weight(1f), label = { Text("Минуты") }, placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !saving, singleLine = true, shape = RoundedCornerShape(16.dp))
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(colors.accentSoft).padding(12.dp)) {
                    Text("Пробуждение · ${sleepDate(wakeDay, "d MMMM yyyy")}",
                        style = AppTheme.type.caption, color = colors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(enabled = !saving, onClick = {
                            TimePickerDialog(context, { _, hour, minute ->
                                wakeTime = hour * 60 + minute
                                saveError = null
                            }, wakeTime / 60, wakeTime % 60, true).show()
                        }) { Text(clockText(wakeTime), color = colors.accent) }
                        if (!dateLocked && record == null) TextButton(enabled = !saving, onClick = {
                            val date = calendarAt(wakeDay)
                            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                                wakeDay = Calendar.getInstance().apply { clear(); set(year, month, dayOfMonth) }.timeInMillis
                                saveError = null
                            }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).apply {
                                datePicker.maxDate = System.currentTimeMillis()
                            }.show()
                        }) { Text("Дата", color = colors.accent) }
                    }
                    if (validDuration && problem == null) {
                        Text("${sleepDate(start, "d MMM · HH:mm")} → ${clockText(wakeTime)}",
                            style = AppTheme.type.caption, color = colors.textPrimary)
                    }
                }
                (problem ?: saveError)?.let { Text(it, style = AppTheme.type.caption, color = colors.danger) }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving && validDuration && problem == null,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.inkOn),
                onClick = {
                    saving = true
                    scope.launch {
                        try {
                            repository.saveSleep(start, end, editing = record != null)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            saveError = "Не удалось сохранить. Проверь, есть ли запись на эту дату."
                            return@launch
                        } finally {
                            saving = false
                        }
                        onSaved()
                        onDismiss()
                    }
                },
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun SleepRecordRow(record: SleepRecord, today: Long, onEdit: () -> Unit) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.DateRange, null, Modifier.padding(end = 10.dp).size(22.dp), tint = colors.accent)
        Column(Modifier.weight(1f)) {
            Text(when (record.dayStartMillis) {
                today -> "Сегодня"
                offsetDay(today, -1) -> "Вчера"
                else -> sleepDate(record.dayStartMillis, "d MMMM")
            }, style = AppTheme.type.label, color = colors.textPrimary)
            Text("${sleepDate(record.startedAtMillis, "HH:mm")} – ${sleepDate(record.endedAtMillis, "HH:mm")}",
                style = AppTheme.type.caption, color = colors.textSecondary)
        }
        Text(durationText(record.durationMinutes), color = colors.textPrimary, style = AppTheme.type.label)
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, "Редактировать запись сна", tint = colors.accent)
        }
    }
}

@Composable
private fun SleepMetric(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    val colors = AppTheme.colors
    Row(modifier.clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.accentSoft),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = colors.accent)
        }
        Column {
            Text(label, style = AppTheme.type.caption, color = colors.textSecondary)
            Text(value, style = AppTheme.type.h4, color = colors.textPrimary)
        }
    }
}

@Composable
private fun SleepWeekChart(days: List<Long>, week: List<SleepRecord?>, selectedDay: Long) {
    val colors = AppTheme.colors
    val scale = maxOf(sleepGoalMinutes, week.maxOf { it?.durationMinutes ?: 0 }) * 1.22f
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Сон за неделю", style = AppTheme.type.h3, color = colors.textPrimary, modifier = Modifier.weight(1f))
            Text("Цель 8 ч", style = AppTheme.type.caption, color = colors.textSecondary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.width(22.dp).height(104.dp)) {
                for (hour in 0..8 step 2) {
                    Text("$hour ч", Modifier.offset(y = (104.dp * (1f - hour * 60 / scale) - 8.dp)
                        .coerceIn(0.dp, 88.dp)), style = AppTheme.type.captionS, color = colors.textSecondary)
                }
            }
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().height(104.dp)) {
                    Canvas(Modifier.matchParentSize()) {
                        val y = size.height * (1f - sleepGoalMinutes / scale)
                        drawLine(colors.textTertiary, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
                    }
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        week.forEachIndexed { index, record ->
                            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                                if (record != null) {
                                    val fraction = record.durationMinutes / scale
                                    Box(Modifier.fillMaxWidth().fillMaxHeight(fraction)
                                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                        .background(if (days[index] == selectedDay) colors.accent else colors.accent.copy(alpha = 0.42f)))
                                    Text("${record.durationMinutes / 60}:${(record.durationMinutes % 60).toString().padStart(2, '0')}",
                                        modifier = Modifier.offset(y = -(104.dp * fraction + 1.dp)),
                                        color = colors.textPrimary, fontSize = 10.sp, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    days.forEach { day ->
                        Box(Modifier.weight(1f).padding(top = 4.dp), contentAlignment = Alignment.Center) {
                            Text(sleepDate(day, "EE"), color = if (day == selectedDay) colors.accent else colors.textSecondary,
                                style = AppTheme.type.captionS, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
