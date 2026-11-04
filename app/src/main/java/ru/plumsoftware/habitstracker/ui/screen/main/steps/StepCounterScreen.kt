package ru.plumsoftware.habitstracker.ui.screen.main.steps

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import ru.plumsoftware.habitstracker.data.database.WellnessRepository
import ru.plumsoftware.habitstracker.data.service.StepCounterManager
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StepCounterScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository: WellnessRepository = koinInject()
    var steps by remember { mutableIntStateOf(0) }
    var sensorAvailable by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(StepCounterManager.hasPermission(context)) }
    var trackingEnabled by remember { mutableStateOf(StepCounterManager.isEnabled(context)) }
    val colors = AppTheme.colors

    LaunchedEffect(Unit) {
        trackingEnabled = StepCounterManager.isEnabled(context)
        sensorAvailable = (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
            .getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
        repository.stepsForDay().collect { steps = it?.steps ?: 0 }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) { StepCounterManager.start(context); trackingEnabled = true }
    }

    Column(
        modifier.fillMaxSize().background(colors.bgScreen).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹ Назад") }
            Spacer(Modifier.width(8.dp))
            Text("Шагомер", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("СЕГОДНЯ", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                Spacer(Modifier.height(12.dp))
                Text(NumberFormat.getIntegerInstance(Locale.getDefault()).format(steps),
                    style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = colors.accent)
                Text("шагов", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(
                    progress = { (steps / 10_000f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = colors.accent, trackColor = colors.accentSoft
                )
                Spacer(Modifier.height(8.dp))
                Text("${(steps * 100 / 10_000).coerceAtMost(100)}% от цели 10 000 шагов", color = colors.textSecondary)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.surface), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(when {
                    !sensorAvailable -> "На этом устройстве не найден датчик шагов"
                    !permissionGranted -> "Разреши доступ к физической активности, чтобы считать шаги"
                    else -> "Подсчёт шагов работает автоматически в фоне"
                }, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
                if (!sensorAvailable) {
                    Text("Для автоматического подсчёта нужен встроенный датчик шагомера. Некоторые устройства его не имеют.",
                        color = colors.textSecondary)
                } else if (!permissionGranted) {
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        else { StepCounterManager.start(context); trackingEnabled = true }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Разрешить и включить") }
                } else {
                    Button(onClick = {
                        if (trackingEnabled) StepCounterManager.stop(context) else StepCounterManager.start(context)
                        trackingEnabled = !trackingEnabled
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (trackingEnabled) "Остановить шагомер" else "Включить шагомер")
                    }
                }
            }
        }
        Text("Данные сохраняются на устройстве. Для фонового подсчёта Android показывает постоянное уведомление. Работа зависит от наличия датчика и ограничений энергосбережения производителя.",
            color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
    }
}
