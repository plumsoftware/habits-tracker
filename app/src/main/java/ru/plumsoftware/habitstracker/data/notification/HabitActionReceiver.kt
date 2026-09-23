package ru.plumsoftware.habitstracker.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import ru.plumsoftware.habitstracker.data.database.HabitRepository

/**
 * Обрабатывает кнопки в системном уведомлении (ТЗ §16):
 * «Отметить» — закрывает привычку без открытия приложения; «Отложить» — повтор через 30 мин.
 */
class HabitActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val habitId = intent.getStringExtra(HabitNotificationManager.EXTRA_HABIT_ID) ?: return
        val habitTitle = intent.getStringExtra(HabitNotificationManager.EXTRA_HABIT_TITLE) ?: ""
        val notifId = intent.getIntExtra(HabitNotificationManager.EXTRA_NOTIF_ID, 0)

        // Скрываем уведомление сразу
        NotificationManagerCompat.from(context).cancel(notifId)

        val repository: HabitRepository = GlobalContext.get().get()
        val manager: HabitNotificationManager = GlobalContext.get().get()

        when (action) {
            HabitNotificationManager.ACTION_MARK -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repository.markHabitCompleted(habitId, System.currentTimeMillis())
                    } finally {
                        pending.finish()
                    }
                }
            }

            HabitNotificationManager.ACTION_SNOOZE -> {
                manager.snooze(habitId, habitTitle, 30)
            }

            HabitNotificationManager.ACTION_SKIP -> {
                // Пропустить: просто убираем уведомление (уже скрыто выше), больше ничего не делаем
            }
        }
    }
}
