package ru.plumsoftware.habitstracker.ui.screen.form

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.habitstracker.data.model.Category
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.Periodicity
import ru.plumsoftware.habitstracker.ui.components.AppIcon
import ru.plumsoftware.habitstracker.ui.components.HabitIcons
import ru.plumsoftware.habitstracker.ui.components.IconTile
import ru.plumsoftware.habitstracker.ui.components.PrimaryButton
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.theme.softShadow
import java.util.UUID

private enum class Frequency { DAILY, BY_WEEKDAYS, TIMES_PER_WEEK }

// Порядок дней недели пн..вс с их Calendar.DAY_OF_WEEK
private val WEEKDAYS = listOf(
    2 to "пн", 3 to "вт", 4 to "ср", 5 to "чт", 6 to "пт", 7 to "сб", 1 to "вс",
)

private val QUICK_EMOJI = listOf("💧", "🏃", "📚", "🧘", "🥗", "🌙", "🎯")
private val REMIND_OPTIONS = listOf(0, 5, 10, 15, 30, 60)

/**
 * Экран «Новая привычка» / «Настроить привычку» (ТЗ §7). Работает с моделью Room:
 * частота DAILY / «N раз в неделю» (WEEKLY + targetCount), эмодзи, категория,
 * время и «напомнить за», цель в день.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitFormScreen(
    editing: Habit?,
    categories: List<Category>,
    allHabits: List<Habit>,
    onBack: () -> Unit,
    onSave: (Habit) -> Unit,
    onDelete: (Habit) -> Unit,
    onOpenTemplates: () -> Unit = {},
    onCreateCategory: (String, Long) -> Unit = { _, _ -> },
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val isEdit = editing != null

    var emoji by remember { mutableStateOf(editing?.emoji?.ifBlank { "🎯" } ?: "🎯") }
    var title by remember { mutableStateOf(TextFieldValue(editing?.title ?: "")) }
    var categoryId by remember { mutableStateOf(editing?.categoryId ?: Category.ID_HEALTH) }
    var frequency by remember {
        mutableStateOf(
            when {
                editing?.isByWeekdays == true -> Frequency.BY_WEEKDAYS
                editing?.periodicity == Periodicity.WEEKLY -> Frequency.TIMES_PER_WEEK
                else -> Frequency.DAILY
            }
        )
    }
    val weekdaySet = remember { mutableStateOf(editing?.weekdaySet ?: setOf(2, 3, 4, 5, 6)) }
    var timesPerWeek by remember { mutableIntStateOf(if (editing?.periodicity == Periodicity.WEEKLY) editing.targetCount.coerceIn(1, 7) else 3) }
    var hour by remember { mutableIntStateOf(editing?.reminderTimeHour?.coerceAtLeast(0) ?: 8) }
    var minute by remember { mutableIntStateOf(editing?.reminderTimeMinute ?: 0) }
    var hasTime by remember { mutableStateOf((editing?.reminderTimeHour ?: 8) >= 0) }
    var remindBefore by remember { mutableIntStateOf(editing?.reminderInterval ?: 10) }
    var dailyGoal by remember { mutableIntStateOf(if (editing?.periodicity == Periodicity.DAILY) editing.targetCount.coerceIn(1, 12) else 1) }
    var chainedAfterId by remember { mutableStateOf(editing?.chainedAfterId) }
    var freezeEnabled by remember { mutableStateOf(editing?.freezeEnabled ?: false) }
    var titleError by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var pulsedChip by remember { mutableIntStateOf(-1) }
    androidx.compose.runtime.LaunchedEffect(pulsedChip) {
        if (pulsedChip >= 0) { kotlinx.coroutines.delay(130); pulsedChip = -1 }
    }
    val shakeX = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            shakeX.animateTo(0f, androidx.compose.animation.core.keyframes {
                durationMillis = 300
                (-8f) at 40; 8f at 90; (-6f) at 150; 6f at 210; 0f at 300
            })
        }
    }
    var weekdayError by remember { mutableStateOf(false) }

    var showEmojiPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRemindPicker by remember { mutableStateOf(false) }
    var showChainPicker by remember { mutableStateOf(false) }
    var showCreateCategory by remember { mutableStateOf(false) }

    // Кандидаты для связывания: все, кроме текущей и тех, что уже ссылаются на текущую (защита от циклов)
    val chainCandidates = remember(allHabits, editing) {
        allHabits.filter { it.id != editing?.id && it.chainedAfterId != editing?.id }
    }
    val chainedTitle = chainCandidates.firstOrNull { it.id == chainedAfterId }?.title

    fun buildHabit(): Habit {
        val weekly = frequency == Frequency.TIMES_PER_WEEK
        val byWeekdays = frequency == Frequency.BY_WEEKDAYS
        return Habit(
            id = editing?.id ?: UUID.randomUUID().toString(),
            title = title.text.trim(),
            body = editing?.body ?: "",
            periodicity = if (weekly) Periodicity.WEEKLY else Periodicity.DAILY,
            creationDate = editing?.creationDate ?: System.currentTimeMillis(),
            targetCount = if (weekly) timesPerWeek else dailyGoal,
            reminderTimeHour = if (hasTime) hour else -1,
            reminderTimeMinute = if (hasTime) minute else 0,
            reminderInterval = if (hasTime) remindBefore else 0,
            categoryId = categoryId,
            emoji = emoji,
            weekdays = if (byWeekdays) weekdaySet.value.sorted().joinToString(",") else "",
            chainedAfterId = chainedAfterId,
            freezeEnabled = freezeEnabled,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgScreen)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // Хедер
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.screenH, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconSquare(HabitIcons.chevronLeft, onBack, 40.dp, 24.dp)
            Text(if (isEdit) "Настроить привычку" else "Новая привычка", style = AppTheme.type.h2, color = colors.textPrimary)
            Spacer(Modifier.weight(1f))
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = spacing.screenH, end = spacing.screenH, top = 4.dp, bottom = spacing.screenBottomPlain,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
        ) {
            // §7.1 Быстрый старт с шаблона (не для режима редактирования)
            if (!isEdit) {
                item {
                    Card {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickableNoRipple(onClick = onOpenTemplates),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconTile(HabitIcons.sparkles, colors.accent, colors.accentSoft, 44.dp, 20.dp, RoundedCornerShape(15.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Начать с шаблона", style = AppTheme.type.bodyS, color = colors.textPrimary)
                                Text("60+ готовых привычек и подборок", style = AppTheme.type.caption, color = colors.textSecondary)
                            }
                            AppIcon(HabitIcons.chevronRight, tint = colors.accent, size = 16.dp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Или быстрый выбор:", style = AppTheme.type.caption, color = colors.textSecondary)
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(QUICK_TEMPLATES.size) { i ->
                                val t = QUICK_TEMPLATES[i]
                                val chipScale by androidx.compose.animation.core.animateFloatAsState(
                                    if (pulsedChip == i) 0.94f else 1f,
                                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 500f),
                                    label = "chip",
                                )
                                Row(
                                    modifier = Modifier.scale(chipScale).clip(AppTheme.shapes.pill)
                                        .background(categoryColor(t.categoryId, colors).copy(alpha = 0.14f))
                                        .clickableNoRipple(onClick = {
                                            emoji = t.emoji
                                            title = TextFieldValue(t.title)
                                            categoryId = t.categoryId
                                            titleError = false
                                            pulsedChip = i
                                        })
                                        .padding(horizontal = 13.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(t.emoji, fontSize = 15.sp)
                                    Text(t.title, style = AppTheme.type.label, color = colors.textPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // §7.2 Основное
            item {
                Card {
                    val previewColor = categories.firstOrNull { it.id == categoryId }?.let { Color(it.colorArgb) } ?: categoryColor(categoryId, colors)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(previewColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) { Text(emoji, fontSize = 30.sp) }
                        FlowRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            QUICK_EMOJI.forEach { e ->
                                EmojiCell(e, selected = e == emoji) { emoji = e }
                            }
                            Box(
                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(colors.accentSoft)
                                    .clickableNoRipple(onClick = { showEmojiPicker = true }),
                                contentAlignment = Alignment.Center,
                            ) { Text("99+", style = AppTheme.type.captionS, color = colors.accent) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("НАЗВАНИЕ", style = AppTheme.type.overline, color = colors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.offset(x = shakeX.value.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(colors.surfaceMuted)
                            .then(if (titleError) Modifier.border(1.5.dp, colors.danger, RoundedCornerShape(16.dp)) else Modifier)
                            .padding(15.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (title.text.isEmpty()) {
                            Text("Например, пить воду", style = AppTheme.type.body, color = colors.textTertiary)
                        }
                        BasicTextField(
                            value = title,
                            onValueChange = { if (it.text.length <= 48) { title = it; titleError = false } },
                            singleLine = true,
                            textStyle = AppTheme.type.body.copy(color = colors.textPrimary),
                            cursorBrush = SolidColor(colors.accent),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (titleError) {
                        Spacer(Modifier.height(6.dp))
                        Text("Введите название", style = AppTheme.type.caption, color = colors.danger)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("КАТЕГОРИЯ", style = AppTheme.type.overline, color = colors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    val cats = if (categories.isEmpty()) builtinCategories().map { Triple(it.first, it.second, categoryColor(it.first, colors)) }
                        else categories.map { Triple(it.id, it.name, Color(it.colorArgb)) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cats.forEach { (id, name, color) ->
                            CategoryChip(name = name, color = color, selected = id == categoryId) { categoryId = id }
                        }
                        // Чип создания новой категории
                        Row(
                            modifier = Modifier.clip(AppTheme.shapes.pill).background(colors.surfaceMuted)
                                .clickableNoRipple(onClick = { showCreateCategory = true })
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            AppIcon(HabitIcons.plus, tint = colors.accent, size = 14.dp)
                            Text("Категория", style = AppTheme.type.label, color = colors.accent)
                        }
                    }
                }
            }

            // §7.3 Расписание
            item {
                Card {
                    Text("Расписание", style = AppTheme.type.h5, color = colors.textPrimary)
                    Spacer(Modifier.height(16.dp))
                    // Частота
                    Segmented(
                        options = listOf("Каждый день", "По дням", "N раз/нед"),
                        selectedIndex = when (frequency) {
                            Frequency.DAILY -> 0; Frequency.BY_WEEKDAYS -> 1; Frequency.TIMES_PER_WEEK -> 2
                        },
                        onSelect = {
                            frequency = when (it) { 0 -> Frequency.DAILY; 1 -> Frequency.BY_WEEKDAYS; else -> Frequency.TIMES_PER_WEEK }
                        },
                    )
                    if (frequency == Frequency.BY_WEEKDAYS) {
                        Spacer(Modifier.height(12.dp))
                        WeekdayRow(
                            selected = weekdaySet.value,
                            onToggle = { day ->
                                val next = weekdaySet.value.toMutableSet()
                                if (next.contains(day)) {
                                    if (next.size > 1) next.remove(day) else weekdayError = true
                                } else { next.add(day); weekdayError = false }
                                weekdaySet.value = next
                            },
                        )
                        if (weekdayError) {
                            Spacer(Modifier.height(6.dp))
                            Text("Выберите хотя бы один день", style = AppTheme.type.caption, color = colors.danger)
                        }
                    }
                    if (frequency == Frequency.TIMES_PER_WEEK) {
                        Spacer(Modifier.height(12.dp))
                        StepperTile(
                            label = "Раз в неделю", subtitle = "Сколько дней из 7",
                            value = timesPerWeek, min = 1, max = 7,
                            onMinus = { timesPerWeek = (timesPerWeek - 1).coerceAtLeast(1) },
                            onPlus = { timesPerWeek = (timesPerWeek + 1).coerceAtMost(7) },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Время + напоминание
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        InfoTile(
                            modifier = Modifier.weight(1f), label = "Время",
                            value = if (hasTime) "%02d:%02d".format(hour, minute) else "Весь день",
                            onClick = { showTimePicker = true },
                        )
                        InfoTile(
                            modifier = Modifier.weight(1f), label = "Напомнить за",
                            value = if (!hasTime || remindBefore == 0) "Выкл" else "$remindBefore мин",
                            onClick = { showRemindPicker = true },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Цель в день
                    StepperTile(
                        label = "Цель в день", subtitle = "Сколько раз отметить",
                        value = dailyGoal, min = 1, max = 12,
                        onMinus = { dailyGoal = (dailyGoal - 1).coerceAtLeast(1) },
                        onPlus = { dailyGoal = (dailyGoal + 1).coerceAtMost(12) },
                    )
                }
            }

            // §7.4 Умные опции
            item {
                Card {
                    Text("Умные опции", style = AppTheme.type.h5, color = colors.textPrimary)
                    Spacer(Modifier.height(14.dp))
                    // §7.4.1 Связать с привычкой
                    Row(
                        modifier = Modifier.fillMaxWidth().clickableNoRipple(onClick = { showChainPicker = true }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconTile(HabitIcons.link, colors.notice, colors.noticeSoft, 40.dp, 18.dp, RoundedCornerShape(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Связать с привычкой", style = AppTheme.type.bodyS, color = colors.textPrimary)
                            Text(
                                chainedTitle?.let { "Сразу после «${it.trim()}»" } ?: "Не связана",
                                style = AppTheme.type.caption, color = colors.textSecondary,
                            )
                        }
                        AppIcon(HabitIcons.chevronRight, tint = colors.checkboxOutline, size = 15.dp)
                    }
                    Spacer(Modifier.height(14.dp))
                    // §7.4.2 Заморозка стрика
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconTile(HabitIcons.snowflake, colors.info, colors.infoSoft, 40.dp, 18.dp, RoundedCornerShape(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Заморозка стрика", style = AppTheme.type.bodyS, color = colors.textPrimary)
                            Text("1 пропуск в неделю не сбросит серию", style = AppTheme.type.caption, color = colors.textSecondary)
                        }
                        FormToggle(checked = freezeEnabled) { freezeEnabled = it }
                    }
                }
            }

            // §7.5 Сохранение
            item {
                PrimaryButton(
                    text = if (isEdit) "Сохранить изменения" else "Сохранить привычку",
                    onClick = {
                        if (title.text.isBlank()) { titleError = true; shakeTrigger++ } else onSave(buildHabit())
                    },
                )
            }
            // Удаление привычки — под кнопкой сохранения (только в режиме редактирования)
            if (isEdit) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(AppTheme.shapes.button)
                            .background(colors.dangerSoft)
                            .clickableNoRipple(onClick = { editing?.let(onDelete) })
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(HabitIcons.close, tint = colors.danger, size = 16.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Удалить привычку", style = AppTheme.type.h5, color = colors.danger)
                    }
                }
            }
        }
    }

    if (showCreateCategory) {
        CreateCategorySheet(
            onCreate = { name, color -> onCreateCategory(name, color); showCreateCategory = false },
            onDismiss = { showCreateCategory = false },
        )
    }
    if (showChainPicker) {
        ChainPickerSheet(
            candidates = chainCandidates,
            categories = categories,
            selectedId = chainedAfterId,
            onPick = { chainedAfterId = it; showChainPicker = false },
            onDismiss = { showChainPicker = false },
        )
    }
    if (showEmojiPicker) {
        EmojiPickerSheet(current = emoji, onPick = { emoji = it; showEmojiPicker = false }, onDismiss = { showEmojiPicker = false })
    }
    if (showTimePicker) {
        TimePickerSheet(
            initialHour = hour, initialMinute = minute, hasTime = hasTime,
            onConfirm = { h, m, enabled -> hour = h; minute = m; hasTime = enabled; showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
    }
    if (showRemindPicker) {
        RemindPickerSheet(current = remindBefore, onPick = { remindBefore = it; showRemindPicker = false }, onDismiss = { showRemindPicker = false })
    }
}

// ---------- Строительные блоки ----------

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(AppTheme.elevation.cardRaised, AppTheme.shapes.cardL)
            .clip(AppTheme.shapes.cardL)
            .background(colors.surface)
            .padding(20.dp),
        content = content,
    )
}

@Composable
private fun IconSquare(icon: Int, onClick: () -> Unit, box: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier.size(box).softShadow(AppTheme.elevation.subtle, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp)).background(colors.surface).clickableNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { AppIcon(icon, tint = colors.textPrimary, size = iconSize) }
}

@Composable
private fun EmojiCell(emoji: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp))
            .background(if (selected) colors.accentSoft else colors.surfaceMuted)
            .clickableNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = 18.sp) }
}

@Composable
private fun CategoryChip(name: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .clip(AppTheme.shapes.pill)
            .background(if (selected) color.copy(alpha = 0.16f) else colors.surfaceMuted)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(Modifier.size(8.dp).clip(AppTheme.shapes.pill).background(color))
        Text(name, style = AppTheme.type.label, color = if (selected) color else colors.textSecondary)
    }
}

@Composable
private fun Segmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceMuted).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            val bg by animateColorAsState(if (selected) colors.surface else Color.Transparent, label = "segbg")
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(bg)
                    .clickableNoRipple(onClick = { onSelect(i) }).padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = AppTheme.type.label, color = if (selected) colors.textPrimary else colors.textSecondary)
            }
        }
    }
}

@Composable
private fun StepperTile(label: String, subtitle: String, value: Int, min: Int, max: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.surfaceMuted).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = AppTheme.type.bodyS, color = colors.textPrimary)
            Text(subtitle, style = AppTheme.type.caption, color = colors.textSecondary)
        }
        Row(
            modifier = Modifier.clip(AppTheme.shapes.pill).background(colors.surface).padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StepBtn(HabitIcons.minus, enabled = value > min, bg = colors.surfaceMuted, tint = colors.textPrimary, onClick = onMinus)
            Text("$value", style = AppTheme.type.h5, color = colors.textPrimary, modifier = Modifier.width(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            StepBtn(HabitIcons.plus, enabled = value < max, bg = colors.accent, tint = colors.inkOn, onClick = onPlus)
        }
    }
}

@Composable
private fun StepBtn(icon: Int, enabled: Boolean, bg: Color, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(30.dp).clip(AppTheme.shapes.pill)
            .background(if (enabled) bg else bg.copy(alpha = 0.35f))
            .clickableNoRipple(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { AppIcon(icon, tint = tint, size = 16.dp) }
}

@Composable
private fun InfoTile(modifier: Modifier, label: String, value: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(colors.surfaceMuted)
            .clickableNoRipple(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = AppTheme.type.caption, color = colors.textSecondary)
        Text(value, style = AppTheme.type.h5.copy(fontSize = 17.sp), color = colors.textPrimary)
    }
}

// ---------- Листы ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiPickerSheet(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true), containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Text("Выберите иконку", style = AppTheme.type.h3, color = colors.textPrimary)
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.heightIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(EMOJI_LIBRARY) { e ->
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(15.dp))
                            .background(if (e == current) colors.accentSoft else colors.surfaceSunken)
                            .clickableNoRipple(onClick = { onPick(e) }),
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = 22.sp) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerSheet(initialHour: Int, initialMinute: Int, hasTime: Boolean, onConfirm: (Int, Int, Boolean) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val tpState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true), containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Время напоминания", style = AppTheme.type.h3, color = colors.textPrimary)
            Spacer(Modifier.height(16.dp))
            TimePicker(state = tpState)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                PrimaryButton(
                    text = "Без времени", onClick = { onConfirm(0, 0, false) }, modifier = Modifier.weight(1f),
                    background = colors.surfaceMuted, contentColor = colors.textPrimary, shadow = ru.plumsoftware.habitstracker.ui.theme.ShadowSpec.None,
                )
                PrimaryButton(text = "Готово", onClick = { onConfirm(tpState.hour, tpState.minute, true) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindPickerSheet(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true), containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Text("Напомнить за", style = AppTheme.type.h3, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            REMIND_OPTIONS.forEach { m ->
                val selected = m == current
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (selected) colors.accentSoft else Color.Transparent)
                        .clickableNoRipple(onClick = { onPick(m) }).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (m == 0) "Не напоминать" else "За $m мин",
                        style = AppTheme.type.bodyS, color = colors.textPrimary, modifier = Modifier.weight(1f),
                    )
                    if (selected) AppIcon(HabitIcons.check, tint = colors.accent, size = 16.dp)
                }
            }
        }
    }
}

@Composable
private fun WeekdayRow(selected: Set<Int>, onToggle: (Int) -> Unit) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        WEEKDAYS.forEach { (day, label) ->
            val active = selected.contains(day)
            Box(
                modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (active) colors.accent else colors.surfaceMuted)
                    .clickableNoRipple(onClick = { onToggle(day) }),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = AppTheme.type.label, color = if (active) colors.inkOn else colors.textSecondary) }
        }
    }
}

@Composable
private fun FormToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = AppTheme.colors
    val trackColor by androidx.compose.animation.animateColorAsState(if (checked) colors.accent else colors.checkboxOutline, label = "ft")
    val knob by androidx.compose.animation.core.animateDpAsState(if (checked) 23.dp else 3.dp, label = "fk")
    Box(
        modifier = Modifier.size(width = 50.dp, height = 30.dp).clip(AppTheme.shapes.pill).background(trackColor)
            .clickableNoRipple(onClick = { onChange(!checked) }),
    ) {
        Box(
            modifier = Modifier.offset(x = knob).align(Alignment.CenterStart).size(24.dp)
                .softShadow(AppTheme.elevation.subtle, AppTheme.shapes.pill).clip(AppTheme.shapes.pill).background(Color.White)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChainPickerSheet(
    candidates: List<Habit>,
    categories: List<Category>,
    selectedId: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true), containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Text("После какой привычки?", style = AppTheme.type.h3, color = colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Напоминание придёт сразу после того, как вы отметите выбранную привычку",
                style = AppTheme.type.caption, color = colors.textSecondary)
            Spacer(Modifier.height(12.dp))
            ChainRow(emoji = "✕", title = "Не связывать", selected = selectedId == null) { onPick(null) }
            candidates.forEach { h ->
                ChainRow(emoji = h.emoji.ifBlank { "🎯" }, title = h.title.trim(), selected = selectedId == h.id) { onPick(h.id) }
            }
        }
    }
}

@Composable
private fun ChainRow(emoji: String, title: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.accentSoft else Color.Transparent)
            .clickableNoRipple(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceMuted),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 17.sp) }
        Text(title, style = AppTheme.type.bodyS, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.size(22.dp).clip(AppTheme.shapes.pill)
                .background(if (selected) colors.accent else Color.Transparent)
                .then(if (selected) Modifier else Modifier.border(2.dp, colors.checkboxOutline, AppTheme.shapes.pill)),
            contentAlignment = Alignment.Center,
        ) { if (selected) AppIcon(HabitIcons.check, tint = colors.inkOn, size = 13.dp) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCategorySheet(onCreate: (String, Long) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var colorIndex by remember { mutableIntStateOf(0) }
    val palette = Category.PALETTE
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(true), containerColor = colors.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Text("Новая категория", style = AppTheme.type.h3, color = colors.textPrimary)
            Spacer(Modifier.height(12.dp))
            Text("НАЗВАНИЕ", style = AppTheme.type.overline, color = colors.textSecondary)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceMuted).padding(15.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (name.text.isEmpty()) Text("Например, Спорт", style = AppTheme.type.body, color = colors.textTertiary)
                BasicTextField(
                    value = name, onValueChange = { if (it.text.length <= 24) name = it }, singleLine = true,
                    textStyle = AppTheme.type.body.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accent), modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("ЦВЕТ", style = AppTheme.type.overline, color = colors.textSecondary)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.forEachIndexed { i, argb ->
                    val selected = i == colorIndex
                    Box(
                        modifier = Modifier.size(40.dp).clip(AppTheme.shapes.pill).background(Color(argb))
                            .clickableNoRipple(onClick = { colorIndex = i }),
                        contentAlignment = Alignment.Center,
                    ) { if (selected) AppIcon(HabitIcons.check, tint = Color.White, size = 18.dp) }
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = "Создать категорию",
                onClick = { onCreate(name.text.trim(), palette[colorIndex]) },
                enabled = name.text.isNotBlank(),
            )
        }
    }
}

// ---------- Данные ----------

private data class QuickTemplate(val emoji: String, val title: String, val categoryId: String)

private val QUICK_TEMPLATES = listOf(
    QuickTemplate("💧", "Пить воду", Category.ID_HEALTH),
    QuickTemplate("🏃", "Зарядка", Category.ID_HEALTH),
    QuickTemplate("📚", "Чтение", Category.ID_STUDY),
    QuickTemplate("🧘", "Медитация", Category.ID_PERSONAL),
    QuickTemplate("🌙", "Сон вовремя", Category.ID_HEALTH),
    QuickTemplate("🇬🇧", "Английский", Category.ID_STUDY),
)

private fun builtinCategories(): List<Pair<String, String>> = listOf(
    Category.ID_HEALTH to "Здоровье",
    Category.ID_WORK to "Работа",
    Category.ID_STUDY to "Обучение",
    Category.ID_PERSONAL to "Личное",
)

private fun categoryColor(id: String?, colors: ru.plumsoftware.habitstracker.ui.theme.HabitColors): Color = when (id) {
    Category.ID_HEALTH -> colors.categoryHealth
    Category.ID_WORK -> colors.categoryWork
    Category.ID_STUDY -> colors.categoryLearning
    else -> colors.categoryPersonal
}

private val EMOJI_LIBRARY = listOf(
    "💧", "🏃", "📚", "🧘", "🥗", "🌙", "🎸", "🧹", "🚶", "🏋️", "📵", "🗒", "🇬🇧", "☀️",
    "💪", "✍️", "🥤", "🍎", "😴", "⏰", "🎯", "🚴", "🏊", "⚽️", "🎾", "🏀", "🤸", "🥊",
    "💻", "📊", "📈", "📌", "✉️", "📞", "💼", "🖊", "🧮", "🔬", "🧪", "🎓", "🗣", "📝",
    "🧺", "🍳", "🪴", "🐕", "🐈", "🛏", "🚿", "🧽", "🕯", "🎨", "🎮", "🎬", "🎧", "📷",
    "🧶", "✈️", "🌳", "🎲", "☕️", "🍿", "🌊", "⛺️", "💊", "🦷", "🩺", "🚭", "🧠", "🎯",
)
