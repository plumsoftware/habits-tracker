package ru.plumsoftware.habitstracker.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color

/** Цвета виджетов (совпадают с палитрой приложения). */
object WidgetColors {
    val accent = Color(0xFF3963FA)
    val accentDark = Color(0xFF2E52E0)
    val onAccent = Color(0xFFFFFFFF)
    val onAccentMuted = Color(0xCCFFFFFF)

    val surface = Color(0xFFFFFFFF)
    val onSurface = Color(0xFF0E0F13)
    val onSurfaceMuted = Color(0xFF9296A3)
    val divider = Color(0xFFE7EBF3)
    val checkboxBorder = Color(0xFFCBD3E6)

    fun provider(color: Color) = ColorProvider(color)
}

/**
 * Рисует кольцо прогресса в Bitmap: полупрозрачный трек + белая дуга сверху.
 * Центр прозрачный — фон виджета просвечивает, текст рисуется поверх в Glance.
 */
fun buildProgressRing(
    sizePx: Int,
    rate: Float,
    strokePx: Float
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val pad = strokePx / 2f + 2f
    val rect = RectF(pad, pad, sizePx - pad, sizePx - pad)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = 0x40FFFFFF // белый ~25%
    }
    canvas.drawArc(rect, 0f, 360f, false, trackPaint)

    val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        color = 0xFFFFFFFF.toInt()
    }
    val sweep = (rate.coerceIn(0f, 1f)) * 360f
    if (sweep > 0f) {
        canvas.drawArc(rect, -90f, sweep, false, progressPaint)
    }

    return bitmap
}
