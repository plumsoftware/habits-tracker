package ru.plumsoftware.habitstracker.ui.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.plumsoftware.habitstracker.data.database.WellnessActivity

/**
 * Обновляет виджеты привычек с дебаунсом 300 мс
 * (серия быстрых тапов → одно обновление).
 */
object WidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val revisions = WellnessActivity.entries.associateWith { MutableStateFlow(0L) }
    private val pending = WellnessActivity.entries.associateWith { Channel<Context>(Channel.CONFLATED) }
    private var job: Job? = null

    init {
        pending.forEach { (activity, queue) ->
            scope.launch {
                for (context in queue) {
                    delay(500)
                    updateWellnessNow(context, activity)
                }
            }
        }
    }

    internal fun wellnessUpdates(activity: WellnessActivity): StateFlow<Long> = revisions.getValue(activity)

    fun updateWellness(context: Context, activity: WellnessActivity) {
        pending.getValue(activity).trySend(context.applicationContext)
    }

    private suspend fun updateWellnessNow(context: Context, activity: WellnessActivity) {
        revisions.getValue(activity).update { it + 1 }
        try {
            wellnessWidget(activity).updateAll(context)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w("WidgetUpdater", "Failed to update $activity widget", error)
        }
    }

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
        WellnessActivity.entries.forEach { updateWellnessNow(app, it) }
    }
}
