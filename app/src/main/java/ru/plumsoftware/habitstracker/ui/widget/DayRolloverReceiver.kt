package ru.plumsoftware.habitstracker.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Смена дня / часового пояса / загрузка — перерисовываем все виджеты (ТЗ §2.4).
 * (Точный будильник на 00:00:05 не ставим — периодического апдейта раз в 30 мин
 *  из appwidget-provider + системные события достаточно.)
 */
class DayRolloverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WidgetUpdater.updateAllNow(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
