package ru.plumsoftware.habitstracker.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Мягкая кастомная тень (ТЗ §1.4): большой blur, отрицательный spread, низкая альфа.
 * Тень есть только у того, что «поднято» над фоном — карточки, нав-бар, FAB, листы.
 */
@Immutable
data class ShadowSpec(
    val offsetX: Dp,
    val offsetY: Dp,
    val blur: Dp,
    val spread: Dp,
    val color: Color,
) {
    companion object {
        val None = ShadowSpec(0.dp, 0.dp, 0.dp, 0.dp, Color.Transparent)
    }
}

@Immutable
data class Elevation(
    val none: ShadowSpec = ShadowSpec.None,
    val subtle: ShadowSpec = ShadowSpec(0.dp, 8.dp, 18.dp, (-16).dp, Color(0x1F101840)),
    val card: ShadowSpec = ShadowSpec(0.dp, 16.dp, 34.dp, (-28).dp, Color(0x24101840)),
    val cardRaised: ShadowSpec = ShadowSpec(0.dp, 18.dp, 40.dp, (-28).dp, Color(0x1A101840)),
    val accentGlow: ShadowSpec = ShadowSpec(0.dp, 24.dp, 44.dp, (-24).dp, Color(0x663A5AFF)),
    val accentButton: ShadowSpec = ShadowSpec(0.dp, 18.dp, 34.dp, (-16).dp, Color(0x4D3A5AFF)),
    val inkCard: ShadowSpec = ShadowSpec(0.dp, 26.dp, 50.dp, (-30).dp, Color(0x730E1220)),
    val navBar: ShadowSpec = ShadowSpec(0.dp, 18.dp, 40.dp, (-18).dp, Color(0x1A101840)),
    val sheet: ShadowSpec = ShadowSpec(0.dp, (-8).dp, 40.dp, 0.dp, Color(0x400E1220)),
    val fab: ShadowSpec = ShadowSpec(0.dp, 18.dp, 34.dp, (-12).dp, Color(0x4D3A5AFF)),
)

/** Тёмная тема: тени темнее и чуть плотнее (ТЗ §1.4). */
val DarkElevation = Elevation(
    subtle = ShadowSpec(0.dp, 8.dp, 18.dp, (-16).dp, Color(0x40000000)),
    card = ShadowSpec(0.dp, 16.dp, 34.dp, (-28).dp, Color(0x59000000)),
    cardRaised = ShadowSpec(0.dp, 18.dp, 40.dp, (-28).dp, Color(0x4D000000)),
    accentGlow = ShadowSpec(0.dp, 24.dp, 44.dp, (-24).dp, Color(0x665B77FF)),
    accentButton = ShadowSpec(0.dp, 18.dp, 34.dp, (-16).dp, Color(0x595B77FF)),
    inkCard = ShadowSpec(0.dp, 26.dp, 50.dp, (-30).dp, Color(0x99000000)),
    navBar = ShadowSpec(0.dp, 18.dp, 40.dp, (-18).dp, Color(0x59000000)),
    sheet = ShadowSpec(0.dp, (-8).dp, 40.dp, 0.dp, Color(0x66000000)),
    fab = ShadowSpec(0.dp, 18.dp, 34.dp, (-12).dp, Color(0x595B77FF)),
)

val LocalElevation = staticCompositionLocalOf { Elevation() }

/**
 * Рисует мягкую тень [spec] под контентом по форме [shape].
 * Реализация — blur через [BlurMaskFilter] на нативном canvas с учётом spread.
 */
fun Modifier.softShadow(spec: ShadowSpec, shape: Shape): Modifier {
    if (spec.color.alpha == 0f) return this
    return this.drawBehind {
        val spreadPx = spec.spread.toPx()
        val blurPx = spec.blur.toPx()
        val dx = spec.offsetX.toPx()
        val dy = spec.offsetY.toPx()

        val shadowSize = Size(
            (size.width + spreadPx * 2).coerceAtLeast(0f),
            (size.height + spreadPx * 2).coerceAtLeast(0f),
        )
        if (shadowSize.width <= 0f || shadowSize.height <= 0f) return@drawBehind

        val outline = shape.createOutline(shadowSize, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }

        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = spec.color
                isAntiAlias = true
            }
            val frameworkPaint = paint.asFrameworkPaint().apply {
                this.color = spec.color.toArgb()
                if (blurPx > 0f) {
                    maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                }
            }
            canvas.save()
            canvas.translate(dx - spreadPx, dy - spreadPx)
            canvas.nativeCanvas.drawPath(path.asAndroidPath(), frameworkPaint)
            canvas.restore()
        }
    }
}

/** Читает текущий токен тени из темы (обёртка над [LocalElevation]). */
object Shadows {
    val current: Elevation
        @Composable get() = LocalElevation.current
}
