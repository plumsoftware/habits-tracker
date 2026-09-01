package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun IOSSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val animationDuration = 200
    val thumbSize = 28.dp  // Увеличил тумблер
    val trackHeight = 32.dp // Увеличил высоту трека
    val trackWidth = 52.dp  // Увеличил ширину трека

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - 2.dp else 2.dp,
        animationSpec = tween(durationMillis = animationDuration),
        label = "thumb_animation"
    )

    val onColor = MaterialTheme.colorScheme.primary
    val offColor = MaterialTheme.colorScheme.surfaceVariant
    val trackColor by animateColorAsState(
        targetValue = if (checked) onColor else offColor,
        animationSpec = tween(durationMillis = animationDuration),
        label = "track_color_animation"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) Color.White else Color.White,
        animationSpec = tween(durationMillis = animationDuration),
        label = "thumb_color_animation"
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onCheckedChange(!checked)
            }
    ) {
        // Трек (фон)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = trackColor,
                    shape = CircleShape
                )
        )

        // Тумблер (ползунок)
        Box(
            modifier = Modifier
                .size(thumbSize)
                .offset(x = thumbOffset, y = 2.dp)
                .background(
                    color = thumbColor,
                    shape = CircleShape
                )
        )
    }
}