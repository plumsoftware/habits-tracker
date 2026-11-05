package ru.plumsoftware.habitstracker.ui.screen.templates

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import ru.plumsoftware.habitstracker.R
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.habitstracker.data.model.Category
import ru.plumsoftware.habitstracker.data.model.Habit
import ru.plumsoftware.habitstracker.data.model.HabitActivityLink
import ru.plumsoftware.habitstracker.data.model.Periodicity
import ru.plumsoftware.habitstracker.ui.components.AppIcon
import ru.plumsoftware.habitstracker.ui.components.HabitIcons
import ru.plumsoftware.habitstracker.ui.components.PrimaryButton
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.theme.softShadow
import java.util.UUID

/**
 * Экран «Шаблоны» (ТЗ §6): поиск, готовые подборки, фильтр категорий, каталог шаблонов.
 * Выбор шаблона создаёт привычку через [onAddHabit] и возвращает на «Сегодня».
 */
@Composable
fun TemplatesScreen(
    onBack: () -> Unit,
    onAddHabit: (Habit) -> Unit,
) {
    BackHandler(onBack = onBack)

    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var categoryFilter by remember { mutableStateOf<String?>(null) } // null = Все
    var confirmPack by remember { mutableStateOf<HabitPack?>(null) }

    val filtered = remember(query.text, categoryFilter) {
        TEMPLATE_CATALOG.filter { t ->
            (categoryFilter == null || t.categoryId == categoryFilter) &&
                (query.text.isBlank() ||
                    t.title.contains(query.text, true) || t.subtitle.contains(query.text, true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgScreen)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // §6.1 Хедер
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenH, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .softShadow(AppTheme.elevation.subtle, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
                    .clickableNoRipple(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { AppIcon(HabitIcons.chevronLeft, tint = colors.textPrimary, size = 24.dp) }
            Text("Шаблоны", style = AppTheme.type.h2, color = colors.textPrimary)
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = spacing.screenH, end = spacing.screenH,
                top = 4.dp, bottom = spacing.screenBottomPlain,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
        ) {
            item { SearchField(query = query, onChange = { query = it }) }

            if (query.text.isBlank()) {
                item { Text("Готовые подборки", style = AppTheme.type.h5, color = colors.textPrimary) }
                item { PacksRow(onAddPack = { pack -> confirmPack = pack }) }
            }

            item { CategoryFilter(selected = categoryFilter, onSelect = { categoryFilter = it }) }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        "Ничего не нашлось. Создайте привычку с нуля",
                        style = AppTheme.type.caption, color = colors.textSecondary,
                    )
                }
            } else {
                items(filtered.size) { i ->
                    TemplateRow(template = filtered[i], onAdd = { onAddHabit(filtered[i].toHabit()) })
                }
            }
        }
    }

    // §6.3 Подтверждение подборки с чекбоксами
    confirmPack?.let { pack ->
        PackConfirmSheet(
            pack = pack,
            onConfirm = { chosen ->
                chosen.forEach { onAddHabit(it.toHabit()) }
                confirmPack = null
            },
            onDismiss = { confirmPack = null },
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PackConfirmSheet(pack: HabitPack, onConfirm: (List<HabitTemplate>) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val checked = remember { mutableStateListOf<Boolean>().apply { repeat(pack.templates.size) { add(true) } } }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(true),
        containerColor = colors.surface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Text("${pack.emoji} ${pack.title}", style = AppTheme.type.h3, color = colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(pack.subtitle, style = AppTheme.type.caption, color = colors.textSecondary)
            Spacer(Modifier.height(14.dp))
            pack.templates.forEachIndexed { i, t ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .clickableNoRipple(onClick = { checked[i] = !checked[i] }).padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                            .background(categoryColor(t.categoryId, colors).copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(t.emoji, fontSize = 17.sp) }
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = AppTheme.type.bodyS, color = colors.textPrimary)
                        Text(t.subtitle, style = AppTheme.type.caption, color = colors.textSecondary)
                    }
                    Box(
                        modifier = Modifier.size(24.dp).clip(AppTheme.shapes.pill)
                            .background(if (checked[i]) colors.accent else colors.surfaceMuted),
                        contentAlignment = Alignment.Center,
                    ) { if (checked[i]) AppIcon(HabitIcons.check, tint = colors.inkOn, size = 14.dp) }
                }
            }
            Spacer(Modifier.height(14.dp))
            val chosen = pack.templates.filterIndexed { i, _ -> checked[i] }
            PrimaryButton(
                text = "Добавить ${chosen.size} ${habitsWord(chosen.size)}",
                onClick = { onConfirm(chosen) },
                enabled = chosen.isNotEmpty(),
            )
        }
    }
}

private fun habitsWord(n: Int): String {
    val m100 = n % 100; val m10 = n % 10
    return when {
        m100 in 11..14 -> "привычек"
        m10 == 1 -> "привычку"
        m10 in 2..4 -> "привычки"
        else -> "привычек"
    }
}

// ---------- §6.2 Поиск ----------

@Composable
private fun SearchField(query: TextFieldValue, onChange: (TextFieldValue) -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(AppTheme.elevation.subtle, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppIcon(HabitIcons.search, tint = colors.textSecondary, size = 15.dp)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.text.isEmpty()) {
                Text("Поиск: вода, бег, чтение…", style = AppTheme.type.body, color = colors.textTertiary)
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = AppTheme.type.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------- §6.3 Подборки ----------

@Composable
private fun PacksRow(onAddPack: (HabitPack) -> Unit) {
    val atlas = ImageBitmap.imageResource(R.drawable.habit_pack_illustrations)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        items(TEMPLATE_PACKS.size) { i ->
            PackCard(pack = TEMPLATE_PACKS[i], atlas = atlas, onAdd = { onAddPack(TEMPLATE_PACKS[i]) })
        }
    }
}

@Composable
private fun PackCard(pack: HabitPack, atlas: ImageBitmap, onAdd: () -> Unit) {
    val colors = AppTheme.colors
    val bg = packBg(pack.tone, colors)
    val textColor = packText(pack.tone, colors)
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val illustration = remember(atlas, pack.artIndex) {
        val cell = IntSize(atlas.width / 4, atlas.height / 2)
        BitmapPainter(atlas, IntOffset(pack.artIndex % 4 * cell.width, pack.artIndex / 4 * cell.height), cell)
    }
    Column(
        modifier = Modifier
            .width(186.dp * fontScale)
            .height(236.dp * fontScale)
            .softShadow(AppTheme.elevation.cardRaised, AppTheme.shapes.card)
            .clip(AppTheme.shapes.card)
            .background(bg)
            .padding(16.dp),
    ) {
        Image(illustration, contentDescription = null, modifier = Modifier.size(76.dp))
        Spacer(Modifier.height(10.dp))
        Text(pack.title, style = AppTheme.type.h5, color = textColor, maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(
            pack.subtitle, style = AppTheme.type.caption, color = textColor.copy(alpha = 0.7f),
            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f)) // прижимаем кнопку к низу → все карточки одной высоты
        Row(
            modifier = Modifier
                .clip(AppTheme.shapes.pill)
                .background(colors.accent)
                .clickableNoRipple(onClick = onAdd)
                .heightIn(min = 44.dp)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AppIcon(HabitIcons.plus, tint = colors.inkOn, size = 12.dp)
            Text("Добавить все", style = AppTheme.type.label, color = colors.inkOn)
        }
    }
}

// ---------- §6.4 Фильтр категорий ----------

@Composable
private fun CategoryFilter(selected: String?, onSelect: (String?) -> Unit) {
    val colors = AppTheme.colors
    val cats = listOf(
        null to "Все",
        Category.ID_HEALTH to "Здоровье",
        Category.ID_WORK to "Работа",
        Category.ID_STUDY to "Обучение",
        Category.ID_PERSONAL to "Личное",
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(cats.size) { i ->
            val (id, name) = cats[i]
            val active = selected == id
            val bg by animateColorAsState(if (active) colors.ink else colors.surface, label = "catbg")
            Text(
                text = name,
                style = AppTheme.type.label,
                color = if (active) colors.inkOn else colors.textPrimary,
                modifier = Modifier
                    .softShadow(AppTheme.elevation.subtle, AppTheme.shapes.pill)
                    .clip(AppTheme.shapes.pill)
                    .background(bg)
                    .clickableNoRipple(onClick = { onSelect(id) })
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

// ---------- §6.5 Список шаблонов ----------

@Composable
private fun TemplateRow(template: HabitTemplate, onAdd: () -> Unit) {
    val colors = AppTheme.colors
    val catColor = categoryColor(template.categoryId, colors)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(AppTheme.elevation.card, AppTheme.shapes.cardS)
            .clip(AppTheme.shapes.cardS)
            .background(colors.surface)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(catColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Text(template.emoji, fontSize = 22.sp) }
        Column(Modifier.weight(1f)) {
            Text(template.title, style = AppTheme.type.body, color = colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(template.subtitle, style = AppTheme.type.caption, color = colors.textSecondary)
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accentSoft)
                .clickableNoRipple(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) { AppIcon(HabitIcons.plus, tint = colors.accent, size = 17.dp) }
    }
}

// ---------- Модель шаблонов и каталог ----------

private data class HabitTemplate(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val categoryId: String,
    val targetCount: Int = 1,
    val hour: Int = 8,
    val minute: Int = 0,
    val reminderInterval: Int = 10,
    val activityLink: String? = null,
) {
    fun toHabit(): Habit = Habit(
        id = UUID.randomUUID().toString(),
        title = title,
        body = "",
        periodicity = Periodicity.DAILY,
        creationDate = System.currentTimeMillis(),
        targetCount = targetCount,
        reminderTimeHour = hour,
        reminderTimeMinute = minute,
        reminderInterval = reminderInterval,
        categoryId = categoryId,
        emoji = emoji,
        activityLink = activityLink,
    )
}

private enum class PackTone { INK, YELLOW, BLUE, LILAC, MINT, PEACH }

private data class HabitPack(
    val artIndex: Int,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val tone: PackTone,
    val templates: List<HabitTemplate>,
)

private val TEMPLATE_CATALOG = listOf(
    HabitTemplate("💧", "Пить воду", "8 стаканов · каждый день", Category.ID_HEALTH, targetCount = 8, hour = 8),
    HabitTemplate("🏃", "Утренняя зарядка", "15 мин · каждый день · 07:00", Category.ID_HEALTH, hour = 7, activityLink = HabitActivityLink.WORKOUT),
    HabitTemplate("📚", "Читать 20 страниц", "каждый день · 21:30", Category.ID_STUDY, hour = 21, minute = 30),
    HabitTemplate("🧘", "Медитация", "10 мин · каждый день", Category.ID_PERSONAL, hour = 22),
    HabitTemplate("🚶", "10 000 шагов", "каждый день", Category.ID_HEALTH, hour = 9),
    HabitTemplate("🌙", "Спать до 23:00", "каждый день · напоминание", Category.ID_HEALTH, hour = 22, minute = 30, activityLink = HabitActivityLink.SLEEP),
    HabitTemplate("📵", "Без телефона час до сна", "каждый день · 22:00", Category.ID_PERSONAL, hour = 22),
    HabitTemplate("🗒", "План на день", "будни · 09:00", Category.ID_WORK, hour = 9),
    HabitTemplate("🇬🇧", "Английский 15 мин", "каждый день · 20:00", Category.ID_STUDY, hour = 20),
    HabitTemplate("🥗", "Овощи в каждый приём", "3 раза в день", Category.ID_HEALTH, targetCount = 3, hour = 13),
)

private val TEMPLATE_PACKS = listOf(
    HabitPack(
        0, "🌅", "Утренний ритуал", "Заряд бодрости с утра", PackTone.INK,
        listOf(
            TEMPLATE_CATALOG[0], // Пить воду
            TEMPLATE_CATALOG[1], // Зарядка
            TEMPLATE_CATALOG[7], // План на день
        ),
    ),
    HabitPack(
        1, "🧠", "Фокус и учёба", "Меньше отвлечений, больше знаний", PackTone.YELLOW,
        listOf(
            TEMPLATE_CATALOG[2], // Читать
            TEMPLATE_CATALOG[8], // Английский
            TEMPLATE_CATALOG[6], // Без телефона
        ),
    ),
    HabitPack(
        2, "😴", "Здоровый сон", "Ложиться вовремя", PackTone.BLUE,
        listOf(
            TEMPLATE_CATALOG[5], // Спать до 23
            TEMPLATE_CATALOG[6], // Без телефона
            TEMPLATE_CATALOG[3], // Медитация
        ),
    ),
    HabitPack(3, "👟", "Больше движения", "Зарядка и шаги каждый день", PackTone.BLUE,
        listOf(TEMPLATE_CATALOG[1], TEMPLATE_CATALOG[4])),
    HabitPack(4, "💧", "Вода и питание", "Вода и овощи каждый день", PackTone.MINT,
        listOf(TEMPLATE_CATALOG[0], TEMPLATE_CATALOG[9])),
    HabitPack(5, "🕯", "Спокойный вечер", "Чтение и пауза без экрана", PackTone.PEACH,
        listOf(TEMPLATE_CATALOG[2], TEMPLATE_CATALOG[3], TEMPLATE_CATALOG[6])),
    HabitPack(6, "🗒", "План на день", "Планирование и время на учёбу", PackTone.LILAC,
        listOf(TEMPLATE_CATALOG[7], TEMPLATE_CATALOG[8])),
)

private fun packBg(tone: PackTone, colors: ru.plumsoftware.habitstracker.ui.theme.HabitColors): Color = when (tone) {
    PackTone.INK -> colors.ink
    PackTone.YELLOW -> colors.tileYellow
    PackTone.BLUE -> colors.tileBlue
    PackTone.LILAC -> colors.tileLilac
    PackTone.MINT -> colors.tileMint
    PackTone.PEACH -> colors.tilePeach
}

private fun packText(tone: PackTone, colors: ru.plumsoftware.habitstracker.ui.theme.HabitColors): Color = when (tone) {
    PackTone.INK -> colors.inkOn
    PackTone.YELLOW -> colors.tileYellowText
    PackTone.BLUE -> colors.tileBlueText
    PackTone.LILAC -> colors.tileLilacText
    PackTone.MINT -> colors.textPrimary
    PackTone.PEACH -> colors.tilePeachText
}

private fun categoryColor(id: String, colors: ru.plumsoftware.habitstracker.ui.theme.HabitColors): Color = when (id) {
    Category.ID_HEALTH -> colors.categoryHealth
    Category.ID_WORK -> colors.categoryWork
    Category.ID_STUDY -> colors.categoryLearning
    else -> colors.categoryPersonal
}
