package ru.plumsoftware.habitstracker.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Кривые и пружины анимаций (ТЗ §1.6). Длительности — в [Motion] (числа мс),
 * чтобы использовать в `tween(AppTheme.motion.normal, easing = EaseStandard)`.
 */
val EaseStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val EaseOut: Easing = FastOutSlowInEasing

fun <T> springBouncy() = spring<T>(dampingRatio = 0.55f, stiffness = 420f)
fun <T> springSoft() = spring<T>(dampingRatio = 0.85f, stiffness = 300f)

// Значения жёсткости/демпфирования для случаев, где нужен именно spec.
const val SpringBouncyDamping = 0.55f
const val SpringBouncyStiffness = 420f
const val SpringSoftDamping = 0.85f
const val SpringSoftStiffness = 300f
val SpringDefaultStiffness = Spring.StiffnessMedium
