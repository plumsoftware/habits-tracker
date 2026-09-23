package ru.plumsoftware.habitstracker.ui.widget

import android.content.Context
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import ru.plumsoftware.habitstracker.data.database.CategoryRepository
import ru.plumsoftware.habitstracker.data.database.HabitRepository
import ru.plumsoftware.habitstracker.data.settings.SettingsRepository
import ru.plumsoftware.habitstracker.data.settings.WeekStart
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

/** Состояние дня в недельной полосе виджета «Серия». */
enum class DaySquare { DONE, PARTIAL, FROZEN, EMPTY }

/** Одна привычка в виджетах. */
data class WidgetHabit(
    val id: String,
    val title: String,
    val time: String,          // «21:00» или «Весь день»
    val completed: Boolean,
    val emoji: String = "🎯",
    val categoryArgb: Long = 0xFF3A5AFF,
    val goal: Int = 1,
    val count: Int = 0,
    val chainAfterTitle: String? = null,
)

/** День недели в полосе (подпись + состояние). */
data class WidgetDay(val label: String, val state: DaySquare)

/** Снимок данных для всех виджетов. */
data class WidgetData(
    val dateLabel: String = "",     // «пт, 21 ноя»
    val dateShort: String = "",     // «пн, 21»
    val weekdayFull: String = "",   // «понедельник»
    val total: Int = 0,
    val completed: Int = 0,
    val completionRate: Float = 0f, // 0..1
    val streak: Int = 0,
    val habits: List<WidgetHabit> = emptyList(),
    val week: List<WidgetDay> = emptyList(),
)

object WidgetDataLoader {

    private fun repository(): HabitRepository = GlobalContext.get().get()
    private fun categoryRepo(): CategoryRepository = GlobalContext.get().get()
    private fun settingsRepo(): SettingsRepository = GlobalContext.get().get()

    private const val DAY_MS = 24 * 60 * 60 * 1000L

    suspend fun load(): WidgetData {
        return try {
            val repo = repository()
            val today = startOfDay(System.currentTimeMillis())
            val categories = categoryRepo().getAll().first().associateBy { it.id }
            val settings = settingsRepo().current()
            val firstDow = if (settings.weekStart == WeekStart.SUNDAY) Calendar.SUNDAY else Calendar.MONDAY

            val habitsWithStatus = repo.getHabitsWithStatus(today).first()
            // Кэш выполненных дней по каждой привычке (для стрика и недельной полосы)
            val allHabits = repo.getAllHabits().first()
            val completedDaysByHabit = HashMap<String, Set<Long>>()
            for (h in allHabits) {
                completedDaysByHabit[h.id] = repo.habitCompletionDao.getAllCompletionsForHabit(h.id)
                    .filter { it.completed }.map { startOfDay(it.completionDate) }.toSet()
            }

            val habits = habitsWithStatus.map { hws ->
                val h = hws.habit
                val cat = h.categoryId?.let { categories[it] }
                val parent = h.chainedAfterId?.let { pid -> allHabits.firstOrNull { it.id == pid } }
                val parentDone = parent?.let { p -> habitsWithStatus.firstOrNull { it.habit.id == p.id }?.completed == true } ?: true
                WidgetHabit(
                    id = h.id,
                    title = h.title.trim(),
                    time = if (h.reminderTimeHour >= 0) "%02d:%02d".format(h.reminderTimeHour, h.reminderTimeMinute) else "Весь день",
                    completed = hws.completed == true,
                    emoji = h.emoji.ifBlank { "🎯" },
                    categoryArgb = cat?.colorArgb ?: 0xFF3A5AFF,
                    goal = h.targetCount.coerceAtLeast(1),
                    count = hws.completedCount,
                    chainAfterTitle = if (parent != null && !parentDone) parent.title.trim() else null,
                )
            }.sortedWith(compareBy({ it.completed }, { it.time }))

            val total = habits.size
            val completed = habits.count { it.completed }
            val rate = if (total > 0) completed.toFloat() / total else 0f

            WidgetData(
                dateLabel = dateLabel(today),
                dateShort = dateShort(today),
                weekdayFull = weekdayFull(today),
                total = total,
                completed = completed,
                completionRate = rate,
                streak = overallStreak(allHabits, completedDaysByHabit, today, repo),
                habits = habits,
                week = weekStrip(allHabits, completedDaysByHabit, today, firstDow, repo),
            )
        } catch (e: Exception) {
            WidgetData(dateLabel = dateLabel(System.currentTimeMillis()), dateShort = dateShort(System.currentTimeMillis()))
        }
    }

    suspend fun refreshAll(context: Context) = WidgetUpdater.updateAll(context)

    /** Переключить выполнение привычки на сегодня (вызывается из виджета). */
    suspend fun toggle(habitId: String, completed: Boolean) {
        val repo = repository()
        val today = startOfDay(System.currentTimeMillis())
        if (completed) repo.markHabitCompleted(habitId, today) else repo.unmarkHabitCompleted(habitId, today)
    }

    /** Общий стрик: подряд дни (до сегодня), где закрыты ВСЕ запланированные привычки. */
    private fun overallStreak(
        allHabits: List<ru.plumsoftware.habitstracker.data.model.Habit>,
        completedDaysByHabit: Map<String, Set<Long>>,
        today: Long,
        repo: HabitRepository,
    ): Int {
        fun isGoodDay(d: Long): Boolean {
            val scheduled = allHabits.filter { repo.isActiveOn(it, d) }
            return scheduled.isNotEmpty() && scheduled.all { completedDaysByHabit[it.id]?.contains(d) == true }
        }
        var cursor = if (isGoodDay(today)) today else today - DAY_MS
        var streak = 0
        while (isGoodDay(cursor)) { streak++; cursor -= DAY_MS }
        return streak
    }

    private fun weekStrip(
        allHabits: List<ru.plumsoftware.habitstracker.data.model.Habit>,
        completedDaysByHabit: Map<String, Set<Long>>,
        today: Long,
        firstDow: Int,
        repo: HabitRepository,
    ): List<WidgetDay> {
        val cal = Calendar.getInstance().apply { timeInMillis = today; firstDayOfWeek = firstDow }
        while (cal.get(Calendar.DAY_OF_WEEK) != firstDow) cal.add(Calendar.DAY_OF_MONTH, -1)
        val labels = mapOf(
            Calendar.MONDAY to "пн", Calendar.TUESDAY to "вт", Calendar.WEDNESDAY to "ср",
            Calendar.THURSDAY to "чт", Calendar.FRIDAY to "пт", Calendar.SATURDAY to "сб", Calendar.SUNDAY to "вс",
        )
        return (0 until 7).map { i ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
            val d = startOfDay(c.timeInMillis)
            val label = labels[c.get(Calendar.DAY_OF_WEEK)] ?: ""
            val state = when {
                d > today -> DaySquare.EMPTY
                else -> {
                    val scheduled = allHabits.filter { repo.isActiveOn(it, d) }
                    val done = scheduled.count { completedDaysByHabit[it.id]?.contains(d) == true }
                    when {
                        scheduled.isEmpty() -> DaySquare.EMPTY
                        done == scheduled.size -> DaySquare.DONE
                        done > 0 -> DaySquare.PARTIAL
                        else -> DaySquare.EMPTY
                    }
                }
            }
            WidgetDay(label, state)
        }
    }

    private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun dateLabel(timestamp: Long): String =
        SimpleDateFormat("EEE, d MMM", Locale("ru")).format(timestamp).replaceFirstChar { it.lowercase() }

    private fun dateShort(timestamp: Long): String =
        SimpleDateFormat("EEE, d", Locale("ru")).format(timestamp).replaceFirstChar { it.lowercase() }

    private fun weekdayFull(timestamp: Long): String =
        SimpleDateFormat("EEEE", Locale("ru")).format(timestamp)
}
