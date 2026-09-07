package ru.plumsoftware.habitstracker.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import ru.plumsoftware.habitstracker.data.database.HabitRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Русское склонение слова «день» для числа. */
fun daysWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "дней"
        mod10 == 1 -> "день"
        mod10 in 2..4 -> "дня"
        else -> "дней"
    }
}

/** Одна привычка в виджете-списке. */
data class WidgetHabit(
    val id: String,
    val title: String,
    val time: String,      // «21:00»
    val completed: Boolean
)

/** Снимок данных для обоих виджетов. */
data class WidgetData(
    val dateLabel: String = "",     // «пт, 21 ноя»
    val total: Int = 0,
    val completed: Int = 0,
    val completionRate: Float = 0f, // 0..1
    val streak: Int = 0,
    val habits: List<WidgetHabit> = emptyList()
)

object WidgetDataLoader {

    private fun repository(): HabitRepository =
        GlobalContext.get().get()

    suspend fun load(): WidgetData {
        return try {
            val repo = repository()
            val startOfToday = startOfDay(System.currentTimeMillis())

            val habitsWithStatus = repo.getHabitsWithStatus(startOfToday).first()

            val habits = habitsWithStatus.map { hws ->
                val h = hws.habit
                WidgetHabit(
                    id = h.id,
                    title = h.title.trim(),
                    time = "%02d:%02d".format(h.reminderTimeHour, h.reminderTimeMinute),
                    completed = hws.completed == true
                )
            }

            val total = habits.size
            val completed = habits.count { it.completed }
            val rate = if (total > 0) completed.toFloat() / total else 0f

            WidgetData(
                dateLabel = dateLabel(startOfToday),
                total = total,
                completed = completed,
                completionRate = rate,
                streak = calculateStreak(repo, startOfToday),
                habits = habits
            )
        } catch (e: Exception) {
            WidgetData(dateLabel = dateLabel(System.currentTimeMillis()))
        }
    }

    /** Перерисовать все экземпляры обоих виджетов (например, после изменений в приложении). */
    suspend fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        // Каждый виджет обновляем независимо, чтобы сбой одного не мешал другому
        try {
            TodayProgressWidget().updateAll(appContext)
        } catch (_: Exception) {
        }
        try {
            HabitsListWidget().updateAll(appContext)
        } catch (_: Exception) {
        }
    }

    /** Переключить выполнение привычки на сегодня (вызывается из чекбокса виджета). */
    suspend fun toggle(habitId: String, completed: Boolean) {
        val repo = repository()
        val startOfToday = startOfDay(System.currentTimeMillis())
        if (completed) {
            repo.markHabitCompleted(habitId, startOfToday)
        } else {
            repo.unmarkHabitCompleted(habitId, startOfToday)
        }
    }

    /** Текущая серия: подряд идущие дни (до сегодня), где выполнена хотя бы одна привычка. */
    private suspend fun calculateStreak(repo: HabitRepository, startOfToday: Long): Int {
        val allHabits = repo.getAllHabits().first()
        val completedDays = HashSet<Long>()
        for (habit in allHabits) {
            val completions = repo.habitCompletionDao.getAllCompletionsForHabit(habit.id)
            completions.filter { it.completed }
                .forEach { completedDays.add(startOfDay(it.completionDate)) }
        }

        val dayMs = 24 * 60 * 60 * 1000L
        var cursor = startOfToday
        var streak = 0
        while (completedDays.contains(cursor)) {
            streak++
            cursor -= dayMs
        }
        return streak
    }

    private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun dateLabel(timestamp: Long): String {
        val fmt = SimpleDateFormat("EEE, d MMM", Locale("ru"))
        return fmt.format(timestamp).replaceFirstChar { it.lowercase() }
    }
}
