package ru.plumsoftware.habitstracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Отступы, размеры и сетка (ТЗ §1.3). Доступ — через [AppTheme.spacing].
 */
@Immutable
data class Spacing(
    // Базовая шкала
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 6.dp,
    val sm: Dp = 8.dp,
    val m: Dp = 11.dp,
    val md: Dp = 13.dp,
    val l: Dp = 16.dp,
    val lg: Dp = 18.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val huge: Dp = 28.dp,
    // Экран
    val screenH: Dp = 20.dp,
    val screenTop: Dp = 8.dp,
    val screenBottomNav: Dp = 120.dp,
    val screenBottomPlain: Dp = 40.dp,
    val sectionGap: Dp = 16.dp,
    val listGap: Dp = 11.dp,
    val rowGap: Dp = 14.dp,
    val cardPad: Dp = 18.dp,
    val cardPadLarge: Dp = 20.dp,
    // Нижняя навигация / FAB
    val navBarPadH: Dp = 14.dp,
    val navBarPadV: Dp = 11.dp,
    val navBarMargin: Dp = 16.dp,
    val fabSize: Dp = 60.dp,
    val fabOffsetFromNav: Dp = 74.dp,
    val touchMin: Dp = 48.dp,
    // Фиксированные размеры элементов
    val checkbox: Dp = 36.dp,
    val habitIcon: Dp = 50.dp,
    val settingsRowIcon: Dp = 38.dp,
    val templateIcon: Dp = 46.dp,
    val toggleWidth: Dp = 50.dp,
    val toggleHeight: Dp = 30.dp,
    val toggleKnob: Dp = 24.dp,
    val progressRing: Dp = 88.dp,
    val progressRingStroke: Dp = 9.dp,
    val heatCell: Dp = 11.dp,
    val heatGap: Dp = 4.dp,
    val detailChartHeight: Dp = 130.dp,
    val analyticsChartHeight: Dp = 150.dp,
    val habitProgressBar: Dp = 7.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

/**
 * Длительности и easing анимаций (ТЗ §1.6). Доступ — через [AppTheme.motion].
 */
@Immutable
data class Motion(
    val instant: Int = 120,
    val fast: Int = 180,
    val normal: Int = 220,
    val slow: Int = 300,
    val lazy: Int = 420,
)

val LocalMotion = staticCompositionLocalOf { Motion() }
