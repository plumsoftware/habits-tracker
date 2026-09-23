package ru.plumsoftware.habitstracker.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import ru.plumsoftware.habitstracker.data.database.CategoryRepository
import ru.plumsoftware.habitstracker.data.database.HabitRepository
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.ui.components.AppIcon
import ru.plumsoftware.habitstracker.ui.components.HabitIcons
import ru.plumsoftware.habitstracker.ui.components.PrimaryButton
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.theme.softShadow

/** Конфигурация виджета «Быстрая отметка»: выбор до 5 привычек. */
class QuickCheckConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(Activity.RESULT_CANCELED)

        enableEdgeToEdge()
        setContent {
            AppTheme {
                ConfigScreen(
                    onCancel = { finish() },
                    onConfirm = { ids -> saveAndFinish(ids) },
                )
            }
        }
    }

    private fun saveAndFinish(ids: List<String>) {
        CoroutineScope(Dispatchers.Main).launch {
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@QuickCheckConfigActivity).getGlanceIdBy(appWidgetId)
                updateAppWidgetState(this@QuickCheckConfigActivity, glanceId) { prefs ->
                    prefs[QuickCheckState.HABIT_IDS] = ids.joinToString(",")
                }
                QuickCheckWidget().update(this@QuickCheckConfigActivity, glanceId)
            }
            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}

@androidx.compose.runtime.Composable
private fun ConfigScreen(onCancel: () -> Unit, onConfirm: (List<String>) -> Unit) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    var habits by remember { mutableStateOf<List<Habit>>(emptyList()) }
    var catColors by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    val selected = remember { mutableStateListOf<String>() }
    var overLimit by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                val repo: HabitRepository = GlobalContext.get().get()
                val catRepo: CategoryRepository = GlobalContext.get().get()
                habits = repo.getAllHabits().first()
                catColors = catRepo.getAll().first().associate { it.id to it.colorArgb }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.bgScreen).windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).softShadow(AppTheme.elevation.subtle, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp)).background(colors.surface).clickableNoRipple(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) { AppIcon(HabitIcons.chevronLeft, tint = colors.textPrimary, size = 24.dp) }
            Text("Быстрая отметка", style = AppTheme.type.h2, color = colors.textPrimary)
        }
        Text("Выберите до 5 привычек", style = AppTheme.type.caption, color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 20.dp))
        if (overLimit) {
            Text("Не больше 5", style = AppTheme.type.caption, color = colors.danger, modifier = Modifier.padding(horizontal = 20.dp))
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(habits) { habit ->
                val isSel = selected.contains(habit.id)
                val cat = catColors[habit.categoryId]?.let { Color(it) } ?: colors.accent
                Row(
                    modifier = Modifier.fillMaxWidth().clip(AppTheme.shapes.cardS).background(colors.surface)
                        .clickableNoRipple(onClick = {
                            if (isSel) selected.remove(habit.id)
                            else if (selected.size < 5) { selected.add(habit.id); overLimit = false }
                            else overLimit = true
                        }).padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(cat.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(habit.emoji.ifBlank { "🎯" }, fontSize = 18.sp) }
                    Text(habit.title.trim(), style = AppTheme.type.bodyS, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier.size(24.dp).clip(AppTheme.shapes.pill).background(if (isSel) colors.accent else colors.surfaceMuted),
                        contentAlignment = Alignment.Center,
                    ) { if (isSel) AppIcon(HabitIcons.check, tint = colors.inkOn, size = 14.dp) }
                }
            }
        }
        Box(Modifier.padding(20.dp)) {
            PrimaryButton(
                text = "Добавить виджет",
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty(),
            )
        }
    }
}
