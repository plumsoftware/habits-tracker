package ru.plumsoftware.habitstracker.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import ru.plumsoftware.habitstracker.MainActivity
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.data.database.WellnessRepository
import java.util.Calendar

class StepCounterService : Service(), SensorEventListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var repository: WellnessRepository
    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        repository = GlobalContext.get().get()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Ожидаем данные датчика"))
        val sensor = stepSensor ?: run { stopSelf(); return START_NOT_STICKY }
        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val total = event.values.firstOrNull()?.toLong()?.coerceAtLeast(0L) ?: return
        val day = startOfDay(System.currentTimeMillis())
        val savedDay = prefs.getLong(KEY_DAY, -1L)
        val lastTotal = prefs.getLong(KEY_LAST_TOTAL, -1L)
        val currentSteps = prefs.getInt(KEY_TODAY_STEPS, 0)
        val nextSteps = when {
            savedDay != day -> 0
            lastTotal < 0L || total < lastTotal -> currentSteps
            else -> (currentSteps + (total - lastTotal).toInt()).coerceAtLeast(0)
        }
        prefs.edit().putLong(KEY_DAY, day).putLong(KEY_LAST_TOTAL, total)
            .putInt(KEY_TODAY_STEPS, nextSteps).apply()
        scope.launch {
            repository.saveSteps(day, nextSteps)
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification("$nextSteps шагов сегодня"))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onDestroy() { sensorManager.unregisterListener(this); scope.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun startOfDay(time: Long): Long = Calendar.getInstance().run {
        timeInMillis = time
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Шагомер", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Шагомер работает").setContentText(text)
            .setContentIntent(openApp).setOngoing(true).setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE).build()
    }

    companion object {
        const val ACTION_START = "ru.plumsoftware.habitstracker.action.START_STEP_COUNTER"
        private const val PREFS = "step_counter_state"
        private const val KEY_DAY = "day_start"
        private const val KEY_LAST_TOTAL = "last_sensor_total"
        private const val KEY_TODAY_STEPS = "today_steps"
        private const val CHANNEL_ID = "step_counter"
        private const val NOTIFICATION_ID = 7301
    }
}
