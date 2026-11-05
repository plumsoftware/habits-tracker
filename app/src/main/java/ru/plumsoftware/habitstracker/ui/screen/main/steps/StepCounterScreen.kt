package ru.plumsoftware.habitstracker.ui.screen.main.steps

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.data.database.WellnessRepository
import ru.plumsoftware.habitstracker.data.service.StepCounterManager
import ru.plumsoftware.habitstracker.ui.components.AppScreenTopBar
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private fun stepDayStart(time: Long): Long = Calendar.getInstance().apply {
    timeInMillis = time
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun StepCounterScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val repository: WellnessRepository = koinInject()
    val colors = AppTheme.colors
    var today by remember { mutableLongStateOf(stepDayStart(System.currentTimeMillis())) }
    var permissionGranted by remember { mutableStateOf(StepCounterManager.hasPermission(context)) }
    var trackingEnabled by remember { mutableStateOf(StepCounterManager.isEnabled(context)) }
    val sensorAvailable = remember(context) {
        (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }
    var goal by remember { mutableIntStateOf(StepCounterManager.dailyGoal(context)) }
    var editGoal by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf("") }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = StepCounterManager.hasPermission(context)
                trackingEnabled = StepCounterManager.isEnabled(context)
                today = stepDayStart(System.currentTimeMillis())
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); today = stepDayStart(System.currentTimeMillis()) }
    }
    val days = remember(today) { (6 downTo 0).map { offset ->
        Calendar.getInstance().apply { timeInMillis = today; add(Calendar.DAY_OF_YEAR, -offset) }.timeInMillis
    } }
    val historyFlow = remember(repository, today) { repository.stepsHistory(days.first(), today) }
    val history by historyFlow.collectAsState(initial = emptyList())
    val steps = history.firstOrNull { it.dayStartMillis == today }?.steps ?: 0
    val fraction = (steps.toFloat() / goal).coerceIn(0f, 1f)
    fun number(value: Int) = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted) { StepCounterManager.start(context); trackingEnabled = true }
    }

    Column(modifier.fillMaxSize().background(colors.bgScreen).windowInsetsPadding(WindowInsets.safeDrawing)) {
        AppScreenTopBar("Шагомер", onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colors.surface).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Сегодня", color = colors.textSecondary, style = AppTheme.type.bodyS)
                            Text(number(steps), color = colors.textPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                            Text("шагов", color = colors.textSecondary, style = AppTheme.type.bodyS)
                        }
                        Image(painterResource(R.drawable.step_sneaker), null, Modifier.weight(1f).height(150.dp))
                    }
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape),
                        color = colors.accent, trackColor = colors.accentSoft)
                    Text("${(fraction * 100).toInt()}% дневной цели", color = colors.textSecondary, style = AppTheme.type.bodyS)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.accentSoft).padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Твоя цель: ${number(goal)} шагов", modifier = Modifier.weight(1f),
                            color = colors.textPrimary, style = AppTheme.type.caption)
                        IconButton(onClick = { goalInput = goal.toString(); editGoal = true }) {
                            Icon(Icons.Outlined.Edit, "Изменить цель", tint = colors.accent)
                        }
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("Прогресс за неделю", color = colors.textPrimary, style = AppTheme.type.h3)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        days.forEach { day ->
                            val record = history.firstOrNull { it.dayStartMillis == day }
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                                    Canvas(Modifier.fillMaxSize().padding(3.dp)) {
                                        val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                                        drawArc(colors.accentSoft, -90f, 360f, false, style = stroke)
                                        if (record != null) drawArc(colors.accent, -90f,
                                            360f * (record.steps.toFloat() / goal).coerceIn(0f, 1f), false, style = stroke)
                                    }
                                    Text(if (record == null) "–" else "${(record.steps.toLong() * 100 / goal).coerceAtMost(100)}%",
                                        fontSize = 9.sp, color = colors.textPrimary, maxLines = 1)
                                }
                                Text(SimpleDateFormat("EE", Locale("ru")).format(java.util.Date(day)),
                                    color = if (day == today) colors.accent else colors.textSecondary,
                                    style = AppTheme.type.captionS)
                            }
                        }
                    }
                    Text("Цель ${number(goal)} шагов в день", color = colors.textSecondary, style = AppTheme.type.captionS)
                }
            }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.surface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(
                            if (trackingEnabled && permissionGranted && sensorAvailable) colors.success else colors.textTertiary))
                        Text(when {
                            !sensorAvailable -> "Датчик шагов не найден"
                            !permissionGranted -> "Нужен доступ к активности"
                            trackingEnabled -> "Подсчёт включён"
                            else -> "Подсчёт приостановлен"
                        }, color = colors.textPrimary, style = AppTheme.type.bodyS)
                    }
                    if (sensorAvailable) {
                        OutlinedButton(onClick = {
                            if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            else {
                                if (trackingEnabled) StepCounterManager.stop(context) else StepCounterManager.start(context)
                                trackingEnabled = !trackingEnabled
                            }
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text(if (!permissionGranted) "Разрешить и включить" else if (trackingEnabled) "Приостановить" else "Включить подсчёт")
                        }
                    } else Text("Для автоматического подсчёта нужен встроенный датчик шагомера.",
                        color = colors.textSecondary, style = AppTheme.type.caption)
                }
            }
            item {
                TextButton(onClick = { showInfo = !showInfo }, modifier = Modifier.fillMaxWidth()) {
                    Text("ⓘ Данные на устройстве", color = colors.textSecondary)
                }
                if (showInfo) Text("Для фонового подсчёта Android показывает постоянное уведомление. Работа зависит от датчика и настроек энергосбережения устройства.",
                    color = colors.textSecondary, style = AppTheme.type.caption)
            }
        }
    }
    if (editGoal) {
        val parsed = goalInput.toIntOrNull()
        AlertDialog(onDismissRequest = { editGoal = false }, title = { Text("Дневная цель") },
            text = {
                OutlinedTextField(goalInput, { goalInput = it.filter(Char::isDigit).take(6) },
                    label = { Text("Шагов в день") }, supportingText = { Text("От 1 до 100 000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            },
            confirmButton = {
                TextButton(enabled = parsed != null && parsed in 1..100_000, onClick = {
                    parsed?.let { StepCounterManager.setDailyGoal(context, it); goal = it }
                    editGoal = false
                }) { Text("Сохранить") }
            }, dismissButton = { TextButton(onClick = { editGoal = false }) { Text("Отмена") } })
    }
}
