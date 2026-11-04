package ru.plumsoftware.habitstracker.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.plumsoftware.habitstracker.R
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.theme.ShadowSpec
import ru.plumsoftware.habitstracker.ui.theme.softShadow

/** Реестр векторных иконок (ТЗ §15). Системная иконография — только отсюда, не эмодзи. */
object HabitIcons {
    @DrawableRes val bell = R.drawable.ic_bell
    @DrawableRes val chainArrow = R.drawable.ic_chain_arrow
    @DrawableRes val check = R.drawable.ic_check
    @DrawableRes val chevronLeft = R.drawable.ic_chevron_left
    @DrawableRes val chevronRight = R.drawable.ic_chevron_right
    @DrawableRes val clock = R.drawable.ic_clock
    @DrawableRes val close = R.drawable.ic_close
    @DrawableRes val droplet = R.drawable.ic_droplet
    @DrawableRes val export = R.drawable.ic_export
    @DrawableRes val flame = R.drawable.ic_flame
    @DrawableRes val info = R.drawable.ic_info
    @DrawableRes val link = R.drawable.ic_link
    @DrawableRes val minus = R.drawable.ic_minus
    @DrawableRes val moon = R.drawable.ic_moon
    @DrawableRes val navAnalytics = R.drawable.ic_nav_analytics
    @DrawableRes val navAnalyticsFilled = R.drawable.ic_nav_analytics_filled
    @DrawableRes val navCalendar = R.drawable.ic_nav_calendar
    @DrawableRes val navCalendarFilled = R.drawable.ic_nav_calendar_filled
    @DrawableRes val navHome = R.drawable.ic_nav_home
    @DrawableRes val navHomeFilled = R.drawable.ic_nav_home_filled
    @DrawableRes val navSettings = R.drawable.ic_nav_settings
    @DrawableRes val navSettingsFilled = R.drawable.ic_nav_settings_filled
    @DrawableRes val palette = R.drawable.ic_palette
    @DrawableRes val plus = R.drawable.ic_plus
    @DrawableRes val repeat = R.drawable.ic_repeat
    @DrawableRes val search = R.drawable.ic_search
    @DrawableRes val snowflake = R.drawable.ic_snowflake
    @DrawableRes val sparkles = R.drawable.ic_sparkles
    @DrawableRes val target = R.drawable.ic_target
    @DrawableRes val trophy = R.drawable.ic_trophy
    @DrawableRes val tune = R.drawable.ic_tune
    @DrawableRes val volume = R.drawable.ic_volume
}

/** Векторная иконка нужного цвета/размера. */
@Composable
fun AppIcon(
    @DrawableRes res: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(res),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}

/** Квадратная плитка с иконкой (иконки в строках, кнопках). */
@Composable
fun IconTile(
    @DrawableRes res: Int,
    tint: Color,
    background: Color,
    tileSize: Dp,
    iconSize: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(tileSize)
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(res = res, tint = tint, size = iconSize)
    }
}

/** Основная CTA-кнопка на всю ширину (ТЗ §7.5, §4.9): фон accent, тень accentButton. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    background: Color = AppTheme.colors.accent,
    contentColor: Color = AppTheme.colors.inkOn,
    shadow: ShadowSpec = AppTheme.elevation.accentButton,
    textStyle: TextStyle = AppTheme.type.h5,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    val shape = AppTheme.shapes.button
    Row(
        modifier = modifier
            .fillMaxWidth()
            .softShadow(if (enabled) shadow else ShadowSpec.None, shape)
            .clip(shape)
            .background(background)
            .clickableNoRipple(enabled = enabled, onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke(this)
        Text(text = text, style = textStyle, color = contentColor)
    }
}

/** Кликабельность с bounded-ripple по форме. */
@Composable
fun Modifier.clickableRipple(
    shape: Shape,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val indication: Indication = ripple()
    return this
        .clip(shape)
        .clickable(
            interactionSource = interaction,
            indication = indication,
            enabled = enabled,
            onClick = onClick,
        )
}

/** Кликабельность без ripple (для pill-чипов, карточек с собственной анимацией). */
@Composable
fun Modifier.clickableNoRipple(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
}

/** Pill-чип с текстом (фильтры, бейджи). */
@Composable
fun PillChip(
    text: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val shape = AppTheme.shapes.pill
    val base = modifier
        .clip(shape)
        .background(background)
        .let { if (onClick != null) it.clickableNoRipple(onClick = onClick) else it }
        .padding(horizontal = 13.dp, vertical = 8.dp)
    Row(
        modifier = base,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        leading?.invoke()
        Text(text = text, style = AppTheme.type.label, color = contentColor)
    }
}


/** Единая шапка полноэкранных разделов приложения. */
@Composable
fun AppTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = AppTheme.spacing.screenH, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp)
                .softShadow(AppTheme.elevation.subtle, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .clickableNoRipple(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(HabitIcons.chevronLeft, tint = colors.textPrimary, size = 24.dp)
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = AppTheme.type.h2,
            color = colors.textPrimary,
        )
        if (trailing != null) Row(content = trailing)
    }
}
