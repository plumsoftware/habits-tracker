package ru.plumsoftware.habitstracker.ui.screen.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.plumsoftware.habitstracker.ui.components.AppIcon
import ru.plumsoftware.habitstracker.ui.components.HabitIcons
import ru.plumsoftware.habitstracker.ui.components.IconTile
import ru.plumsoftware.habitstracker.ui.components.PillChip
import ru.plumsoftware.habitstracker.ui.components.PrimaryButton
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.theme.EaseStandard
import ru.plumsoftware.habitstracker.ui.theme.softShadow

private data class OnboardPage(
    val emoji: String,
    val haloColor: @Composable () -> Color,
    val chipLeft: String,
    val chipRight: String,
    val title: String,
    val description: String,
)

/**
 * Онбординг (ТЗ §14). Показывается один раз при первом запуске и по кнопке в настройках.
 * [onFinish] — сохранить флаг и уйти на «Сегодня».
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val colors = AppTheme.colors
    val pages = listOf(
        OnboardPage("🌱", { colors.tileBlue }, "🔥 21 день", "💧 8/8",
            "Маленькие шаги — большие перемены",
            "Отмечайте привычки одним касанием и следите, как растут серии."),
        OnboardPage("✨", { colors.tileYellow }, "60+ шаблонов", "🌅 Ритуал",
            "Шаблоны на любой случай",
            "Готовые привычки и подборки: добавляйте пачкой и настраивайте под себя."),
        OnboardPage("📊", { colors.tileGreen }, "↑ 12%", "🏆 30 дней",
            "Стрики и честная аналитика",
            "Видно сильные дни, пропуски и связи между привычками. Без лишних цифр."),
        OnboardPage("🔔", { colors.tileLilac }, "Напоминания", "Точно в 08:00",
            "Разрешите напоминания",
            "Нужно, чтобы уведомления приходили вовремя. Можно изменить в настройках."),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgScreen)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
            .padding(bottom = 30.dp),
    ) {
        // Верх: индикатор прогресса + «Пропустить»
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(pages.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (active) 22.dp else 6.dp)
                            .clip(AppTheme.shapes.pill)
                            .background(if (active) colors.accent else colors.checkboxOutline)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (!isLast) {
                Text(
                    text = "Пропустить",
                    style = AppTheme.type.label,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .clip(AppTheme.shapes.pill)
                        .clickableNoRipple(onClick = onFinish)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { pageIndex ->
            OnboardPageContent(pages[pageIndex], pageIndex == 3)
        }

        PrimaryButton(
            text = if (isLast) "Начать" else "Далее",
            onClick = {
                if (isLast) onFinish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
        )
    }
}

@Composable
private fun OnboardPageContent(page: OnboardPage, isPermissions: Boolean) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Hero(emoji = page.emoji, halo = page.haloColor(), chipLeft = page.chipLeft, chipRight = page.chipRight)
        }

        Text(
            text = page.title,
            style = AppTheme.type.displayS,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = page.description,
            style = AppTheme.type.bodyL,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        if (page.emoji == "✨") {
            Spacer(Modifier.height(20.dp))
            ExampleChips()
        }
        if (isPermissions) {
            Spacer(Modifier.height(20.dp))
            PermissionsBlock()
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Hero(emoji: String, halo: Color, chipLeft: String, chipRight: String) {
    val colors = AppTheme.colors
    val transition = rememberInfiniteTransition(label = "float")
    // Амплитуда плавания чипов = ±8dp (заметно; раньше было ±4px и почти не видно).
    val floatA by transition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2600, easing = EaseStandard), RepeatMode.Reverse),
        label = "a",
    )

    Box(
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Гало
        Box(
            modifier = Modifier
                .size(210.dp)
                .blur(2.dp)
                .clip(CircleShape)
                .background(halo)
        )
        // Плитка с эмодзи
        Box(
            modifier = Modifier
                .size(148.dp)
                .softShadow(AppTheme.elevation.cardRaised, RoundedCornerShape(44.dp))
                .clip(RoundedCornerShape(44.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 66.sp)
        }
        // Плавающие чипы
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 6.dp, top = 20.dp)
                .graphicsLayer { translationY = floatA.dp.toPx() }
                .softShadow(AppTheme.elevation.card, AppTheme.shapes.pill)
                .clip(AppTheme.shapes.pill)
                .background(colors.surface)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(text = chipLeft, style = AppTheme.type.label, color = colors.ink)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 20.dp)
                .graphicsLayer { translationY = -floatA.dp.toPx() }
                .softShadow(AppTheme.elevation.card, AppTheme.shapes.pill)
                .clip(AppTheme.shapes.pill)
                .background(colors.ink)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(text = chipRight, style = AppTheme.type.label, color = colors.inkOn)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ExampleChips() {
    val colors = AppTheme.colors
    val examples = listOf(
        "💧 Вода", "🏃 Зарядка", "📚 Чтение", "🧘 Медитация", "🌙 Сон", "🇬🇧 Английский",
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        examples.forEach { PillChip(text = it, background = colors.surface, contentColor = colors.textPrimary) }
    }
}

private data class PermissionCard(
    val icon: Int,
    val iconTint: @Composable () -> Color,
    val tile: @Composable () -> Color,
    val title: String,
    val description: String,
)

@Composable
private fun PermissionsBlock() {
    val colors = AppTheme.colors
    val context = LocalContext.current

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* результат не критичен */ }

    val cards = listOf(
        PermissionCard(HabitIcons.bell, { colors.accent }, { colors.accentSoft },
            "Уведомления", "Напоминания о привычках"),
        PermissionCard(HabitIcons.clock, { colors.danger }, { colors.dangerSoft },
            "Точные будильники", "Напоминание ровно в срок"),
        PermissionCard(HabitIcons.navSettings, { colors.success }, { colors.successSoft },
            "Работа в фоне", "Без отключения системой"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        cards.forEachIndexed { index, card ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .softShadow(AppTheme.elevation.card, AppTheme.shapes.cardS)
                    .clip(AppTheme.shapes.cardS)
                    .background(colors.surface)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconTile(
                    res = card.icon, tint = card.iconTint(), background = card.tile(),
                    tileSize = 42.dp, iconSize = 20.dp, shape = RoundedCornerShape(14.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(card.title, style = AppTheme.type.bodyS, color = colors.textPrimary)
                    Text(card.description, style = AppTheme.type.caption, color = colors.textSecondary)
                }
                PillChip(
                    text = "Разрешить",
                    background = colors.accent,
                    contentColor = colors.inkOn,
                    onClick = { requestPermission(context, index, notifLauncher::launch) },
                )
            }
        }
    }
}

private fun requestPermission(context: Context, index: Int, requestNotif: (String) -> Unit) {
    when (index) {
        0 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotif(Manifest.permission.POST_NOTIFICATIONS)
        }
        1 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
        2 -> runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            )
        }
    }
}
