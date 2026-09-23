package ru.plumsoftware.habitstracker.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import kotlinx.coroutines.runBlocking
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.data.database.NotificationRepository
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.NotificationType
import ru.plumsoftware.habitstracker.data.model.Periodicity
import ru.plumsoftware.habitstracker.data.settings.SettingsRepository
import ru.plumsoftware.habitstracker.ui.NotificationUtils
import java.util.concurrent.TimeUnit
import java.util.Calendar


class HabitNotificationManager(
    private val context: Context,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository
) {

    companion object {
        const val CHANNEL_ID = "habit_reminders"
        const val ACTION_MARK = "ru.plumsoftware.habitstracker.action.MARK"
        const val ACTION_SNOOZE = "ru.plumsoftware.habitstracker.action.SNOOZE"
        const val ACTION_SKIP = "ru.plumsoftware.habitstracker.action.SKIP"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_TITLE = "extra_habit_title"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
        const val SERIES_RISK_TAG = "series_risk"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о привычках",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о предстоящих привычках"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleHabitNotification(habit: Habit) {
        // Проверяем, есть ли разрешение на уведомления
        if (!NotificationUtils.areNotificationsEnabled(context)) {
            return
        }

        // ТЗ §7.4.1: связанная привычка не планируется по времени —
        // её напоминание приходит по факту закрытия родителя, плюс резерв в 21:00.
        if (habit.chainedAfterId != null) {
            cancelHabitNotification(habit.id)
            scheduleChainFallback(habit, startDay = 0)
            return
        }

        // Отменяем предыдущие уведомления для этой привычки
        cancelHabitNotification(habit.id)

        // Нет конкретного времени («Весь день») — напоминания не шлём.
        // Интервал 0 при этом означает «ровно в назначенное время» (0 минут заранее).
        if (habit.reminderTimeHour < 0) return

        val now = System.currentTimeMillis()

        // Сегодняшнее напоминание: обычно (время − интервал). Если этот момент уже прошёл,
        // но само время привычки ещё впереди сегодня — напомним ровно в назначенное время.
        val notificationTime = calculateNotificationTime(habit)
        val habitTimeToday = habitTimeToday(habit)
        val fireAt = if (notificationTime > now) notificationTime else habitTimeToday
        if (fireAt > now && isScheduledOn(habit, now)) {
            enqueueReminder(habit, fireAt, "habit_${habit.id}")
        }

        // На ближайшие дни/недели
        if (habit.periodicity == Periodicity.WEEKLY) {
            scheduleWeeklyHabitNotifications(habit)
        } else {
            scheduleDailyHabitNotifications(habit)
        }
    }

    /** Ставит одноразовый воркер-напоминание на момент [fireAt]. */
    private fun enqueueReminder(habit: Habit, fireAt: Long, tag: String) {
        val inputData = Data.Builder()
            .putString("habit_id", habit.id)
            .putString("habit_title", habit.title)
            .putString("habit_body", habit.body)
            .putInt("hour", habit.reminderTimeHour)
            .putInt("minute", habit.reminderTimeMinute)
            .putInt("interval", habit.reminderInterval)
            .build()
        val work = OneTimeWorkRequestBuilder<HabitNotificationWorker>()
            .setInitialDelay((fireAt - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(tag)
            .build()
        WorkManager.getInstance(context).enqueue(work)
    }

    /** Сегодняшнее «время привычки» без вычитания интервала. */
    private fun habitTimeToday(habit: Habit): Long = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, habit.reminderTimeHour)
        set(Calendar.MINUTE, habit.reminderTimeMinute)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Запланирована ли привычка на дату [atMillis] (для режима «по дням недели»). */
    private fun isScheduledOn(habit: Habit, atMillis: Long): Boolean {
        if (!habit.isByWeekdays) return true
        val dow = Calendar.getInstance().apply { timeInMillis = atMillis }.get(Calendar.DAY_OF_WEEK)
        return habit.weekdaySet.contains(dow)
    }

    private fun calculateNotificationTime(habit: Habit): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, habit.reminderTimeHour)
            set(Calendar.MINUTE, habit.reminderTimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Вычитаем интервал напоминания
        calendar.add(Calendar.MINUTE, -habit.reminderInterval)

        return calendar.timeInMillis
    }

    private fun scheduleDailyHabitNotifications(habit: Habit) {
        // Планируем уведомления на следующие 7 дней (для «по дням» — только в выбранные дни)
        for (day in 1..7) {
            val notificationTime = calculateNotificationTimeForDay(habit, day)
            if (notificationTime > System.currentTimeMillis() && isScheduledOn(habit, notificationTime)) {
                enqueueReminder(habit, notificationTime, "habit_${habit.id}_future")
            }
        }
    }

    private fun scheduleWeeklyHabitNotifications(habit: Habit) {
        // Планируем уведомления на следующие 4 недели
        for (week in 1..4) {
            val notificationTime = calculateNotificationTimeForWeek(habit, week)

            if (notificationTime > System.currentTimeMillis()) {
                val inputData = Data.Builder()
                    .putString("habit_id", habit.id)
                    .putString("habit_title", habit.title)
                    .putString("habit_body", habit.body)
                    .putInt("interval", habit.reminderInterval)
                    .build()

                val delay = notificationTime - System.currentTimeMillis()

                val workRequest = OneTimeWorkRequestBuilder<HabitNotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag("habit_${habit.id}_future")
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }

    private fun calculateNotificationTimeForDay(habit: Habit, daysFromNow: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, daysFromNow)
            set(Calendar.HOUR_OF_DAY, habit.reminderTimeHour)
            set(Calendar.MINUTE, habit.reminderTimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        calendar.add(Calendar.MINUTE, -habit.reminderInterval)
        return calendar.timeInMillis
    }

    private fun calculateNotificationTimeForWeek(habit: Habit, weeksFromNow: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.WEEK_OF_YEAR, weeksFromNow)
            set(Calendar.HOUR_OF_DAY, habit.reminderTimeHour)
            set(Calendar.MINUTE, habit.reminderTimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        calendar.add(Calendar.MINUTE, -habit.reminderInterval)
        return calendar.timeInMillis
    }

    fun cancelHabitNotification(habitId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("habit_$habitId")
        WorkManager.getInstance(context).cancelAllWorkByTag("habit_${habitId}_future")
    }

    /**
     * Резервное напоминание для связанной привычки в 21:00 (если родитель так и не закрыт).
     * ТЗ §7.4.1 п.2. Планируем на ближайшие 7 дней, начиная с [startDay].
     */
    private fun scheduleChainFallback(habit: Habit, startDay: Int) {
        if (!NotificationUtils.areNotificationsEnabled(context)) return
        for (day in startDay..(startDay + 6)) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_YEAR, day)
                set(Calendar.HOUR_OF_DAY, 21)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val time = calendar.timeInMillis
            if (time <= System.currentTimeMillis()) continue

            val inputData = Data.Builder()
                .putString("habit_id", habit.id)
                .putString("habit_title", habit.title)
                .putString("habit_body", habit.body)
                .putInt("interval", 0)
                .putBoolean("chain_fallback", true)
                .build()

            val work = OneTimeWorkRequestBuilder<HabitNotificationWorker>()
                .setInitialDelay(time - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("habit_${habit.id}_future")
                .build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }

    /**
     * ТЗ §7.4.1 п.4: родитель закрыт — сразу показываем «Следующее: {потомок}».
     * Отменяем сегодняшний резерв 21:00 и переносим его на завтра.
     */
    fun notifyChainNext(child: Habit, parentTitle: String) {
        if (!NotificationUtils.areNotificationsEnabled(context)) return
        val settings = runBlocking { settingsRepository.current() }
        if (!settings.pushEnabled) return
        if (isQuietNow(settings)) { rescheduleAfterQuiet(child, chainFallback = true, endHour = settings.quietEndHour); return }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val body = if (parentTitle.isNotBlank()) "После «$parentTitle» — «${child.title}»" else "Пора: «${child.title}»"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("Следующее: ${child.title}")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        if (!settings.soundEnabled) builder.setSilent(true)
        notificationManager.notify("chain_${child.id}_${System.currentTimeMillis()}".hashCode(), builder.build())

        runBlocking {
            notificationRepository.add(
                title = "Следующее: ${child.title}",
                body = body,
                type = NotificationType.HABIT_REMINDER,
                resourceUrl = ""
            )
        }

        // Резерв на сегодня больше не нужен — переносим на завтра.
        cancelHabitNotification(child.id)
        scheduleChainFallback(child, startDay = 1)
    }

    fun showNotification(habit: Habit) {
        // Проверяем системное разрешение перед показом уведомления
        if (!NotificationUtils.areNotificationsEnabled(context)) {
            return
        }

        // Учитываем пользовательские настройки (push вкл/выкл, звук)
        val settings = runBlocking { settingsRepository.current() }
        if (!settings.pushEnabled) {
            return
        }

        // Не напоминаем о привычке, уже выполненной сегодня
        val alreadyDone = runBlocking {
            runCatching {
                val repo = org.koin.core.context.GlobalContext.get()
                    .get<ru.plumsoftware.habitstracker.data.database.HabitRepository>()
                repo.getHabitStatus(habit.id, System.currentTimeMillis())
            }.getOrDefault(false)
        }
        if (alreadyDone) return

        // ТЗ §11.2/§16: «тихие часы» — откладываем доставку до конца интервала (08:00)
        if (isQuietNow(settings)) {
            rescheduleAfterQuiet(habit, chainFallback = false, endHour = settings.quietEndHour)
            return
        }

        val title = habit.title
        val body = if (habit.reminderInterval > 0)
            "«${habit.title}» — через ${habit.reminderInterval} мин."
        else "Пора: «${habit.title}»"
        val notifId = "${habit.id}_${System.currentTimeMillis()}".hashCode()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(habit.body.ifEmpty { body })
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Отметить", actionIntent(ACTION_MARK, habit, notifId))
            .addAction(0, "Пропустить", actionIntent(ACTION_SKIP, habit, notifId))

        if (!settings.soundEnabled) {
            builder.setSilent(true)
        }

        notificationManager.notify(notifId, builder.build())

        // Сохраняем в БД, чтобы напоминание попало в список «Уведомления»
        runBlocking {
            notificationRepository.add(
                title = "Пора: ${habit.title}",
                body = body,
                type = NotificationType.HABIT_REMINDER,
                resourceUrl = ""
            )
        }
    }

    /** PendingIntent для кнопки-действия в уведомлении (Отметить / Отложить). */
    private fun actionIntent(action: String, habit: Habit, notifId: Int): android.app.PendingIntent {
        val intent = android.content.Intent(context, HabitActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_HABIT_ID, habit.id)
            putExtra(EXTRA_HABIT_TITLE, habit.title)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        return android.app.PendingIntent.getBroadcast(
            context, (habit.id + action).hashCode(), intent, flags
        )
    }

    /** Планирует повторную попытку показа привычки через [minutes] минут (кнопка «Отложить»). */
    fun snooze(habitId: String, habitTitle: String, minutes: Int) {
        val inputData = Data.Builder()
            .putString("habit_id", habitId)
            .putString("habit_title", habitTitle)
            .putString("habit_body", "")
            .putInt("interval", 0)
            .build()
        val work = OneTimeWorkRequestBuilder<HabitNotificationWorker>()
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .setInputData(inputData)
            .addTag("habit_${habitId}_future")
            .build()
        WorkManager.getInstance(context).enqueue(work)
    }

    private fun isQuietNow(settings: ru.plumsoftware.habitstracker.data.settings.AppSettings): Boolean {
        if (!settings.quietHoursEnabled) return false
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = settings.quietStartHour
        val end = settings.quietEndHour
        return if (start <= end) h in start until end else (h >= start || h < end)
    }

    private fun quietEndMillis(endHour: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, endHour)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private fun rescheduleAfterQuiet(habit: Habit, chainFallback: Boolean, endHour: Int) {
        val delay = quietEndMillis(endHour) - System.currentTimeMillis()
        val inputData = Data.Builder()
            .putString("habit_id", habit.id)
            .putString("habit_title", habit.title)
            .putString("habit_body", habit.body)
            .putInt("interval", habit.reminderInterval)
            .putBoolean("chain_fallback", chainFallback)
            .build()
        val work = OneTimeWorkRequestBuilder<HabitNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("habit_${habit.id}_future")
            .build()
        WorkManager.getInstance(context).enqueue(work)
    }

    /**
     * Планирует ежедневную проверку «Серия под угрозой» на ближайшие 20:00 (ТЗ §11.2/§16).
     * Воркер сам перепланирует себя на следующий день.
     */
    fun scheduleSeriesRiskCheck() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        val work = OneTimeWorkRequestBuilder<SeriesRiskWorker>()
            .setInitialDelay(cal.timeInMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .addTag(SERIES_RISK_TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("series_risk_check", androidx.work.ExistingWorkPolicy.REPLACE, work)
    }

    /** Показывает одно уведомление «Серия под угрозой» (учитывая тихие часы). */
    fun showSeriesRisk(count: Int) {
        if (!NotificationUtils.areNotificationsEnabled(context)) return
        val settings = runBlocking { settingsRepository.current() }
        if (!settings.pushEnabled || !settings.seriesEnabled) return
        if (isQuietNow(settings)) return // серия под угрозой уже неактуальна к утру

        val body = "Остались незакрытые привычки с сериями. Не потеряйте прогресс!"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("Серия под угрозой")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        if (!settings.soundEnabled) builder.setSilent(true)
        notificationManager.notify("series_risk".hashCode(), builder.build())
        runBlocking {
            notificationRepository.add(
                title = "Серия под угрозой", body = body,
                type = NotificationType.STREAK, resourceUrl = ""
            )
        }
    }
}