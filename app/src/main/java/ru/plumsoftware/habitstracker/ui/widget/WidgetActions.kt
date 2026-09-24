package ru.plumsoftware.habitstracker.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import ru.plumsoftware.habitstracker.MainActivity

/** Deeplink-адреса (обрабатываются в MainActivity). */
object Deeplinks {
    const val TODAY = "habits://today"
    const val ANALYTICS = "habits://analytics"
    const val NEW_HABIT = "habits://habit/new"
    const val TEMPLATES = "habits://templates"
    fun habit(id: String) = "habits://habit/$id"

    // Deeplink передаётся как extra Intent'а (Glance 1.1.1 не умеет actionStartActivity(Intent))
    val KEY = ActionParameters.Key<String>("deeplink")
    const val EXTRA = "deeplink"
}

/** Открыть приложение по deeplink (через ActionParameters → extra в Intent). */
fun deeplinkAction(context: Context, uri: String): Action =
    actionStartActivity<MainActivity>(actionParametersOf(Deeplinks.KEY to uri))

/** Экшен-параметры отметки. */
object ToggleKeys {
    val habitId = ActionParameters.Key<String>("habitId")
    val completed = ActionParameters.Key<Boolean>("completed")
}

fun toggleAction(habitId: String, completed: Boolean): Action = actionRunCallback<ToggleHabitAction>(
    actionParametersOf(ToggleKeys.habitId to habitId, ToggleKeys.completed to completed)
)

/** Единая логика отметки для всех виджетов: та же, что на экране «Сегодня». */
class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[ToggleKeys.habitId] ?: return
        val completed = parameters[ToggleKeys.completed] ?: return
        WidgetDataLoader.toggle(habitId, completed)
        WidgetUpdater.updateAll(context)
    }
}
