package ru.plumsoftware.habitstracker.data.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import ru.plumsoftware.habitstracker.data.database.WellnessActivity
import ru.plumsoftware.habitstracker.ui.widget.WidgetUpdater

object StepCounterManager {
    private const val PREFS = "step_counter_settings"
    private const val KEY_ENABLED = "tracking_enabled"

    fun dailyGoal(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("daily_goal", 10_000).coerceIn(1, 100_000)

    fun setDailyGoal(context: Context, goal: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("daily_goal", goal.coerceIn(1, 100_000)).apply()
        WidgetUpdater.updateWellness(context, WellnessActivity.STEPS)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun start(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, true).apply()
        ContextCompat.startForegroundService(
            context, Intent(context, StepCounterService::class.java).setAction(StepCounterService.ACTION_START)
        )
    }

    fun stop(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, false).apply()
        context.stopService(Intent(context, StepCounterService::class.java))
    }
}
