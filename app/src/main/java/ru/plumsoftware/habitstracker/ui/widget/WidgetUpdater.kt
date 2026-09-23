package ru.plumsoftware.habitstracker.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Обновляет ВСЕ 4 типа виджетов с дебаунсом 300 мс
 * (серия быстрых тапов → одно обновление).
 */
object WidgetUpdater {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    fun updateAll(context: Context) {
        val app = context.applicationContext
        job?.cancel()
        job = scope.launch {
            delay(300)
            runCatching { TodayProgressWidget().updateAll(app) }
            runCatching { StreakWidget().updateAll(app) }
            runCatching { HabitsListWidget().updateAll(app) }
            runCatching { QuickCheckWidget().updateAll(app) }
        }
    }

    /** Немедленно, без дебаунса (для смены дня/загрузки). */
    suspend fun updateAllNow(context: Context) {
        val app = context.applicationContext
        runCatching { TodayProgressWidget().updateAll(app) }
        runCatching { StreakWidget().updateAll(app) }
        runCatching { HabitsListWidget().updateAll(app) }
        runCatching { QuickCheckWidget().updateAll(app) }
    }
}
