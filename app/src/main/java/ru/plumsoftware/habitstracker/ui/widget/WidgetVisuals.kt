package ru.plumsoftware.habitstracker.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * Цвета виджетов — те же токены, что в приложении (ui/theme/AppColors),
 * со светлой/тёмной парой через ColorProvider(day, night).
 */
object WidgetColors {
    private fun cp(light: Color, dark: Color) = ColorProvider(day = light, night = dark)

    val accent = cp(Color(0xFF3A5AFF), Color(0xFF5B77FF))
    val accentSoft = cp(Color(0xFFE4E9FF), Color(0xFF23305C))
    val onAccent = cp(Color(0xFFFFFFFF), Color(0xFFFFFFFF))
    val onAccentMuted = cp(Color(0xBFFFFFFF), Color(0xBFFFFFFF))

    val bgScreen = cp(Color(0xFFEEF1FA), Color(0xFF0B0E17))
    val surface = cp(Color(0xFFFFFFFF), Color(0xFF161A24))
    val surfaceMuted = cp(Color(0xFFF1F4FC), Color(0xFF1E2330))
    val ink = cp(Color(0xFF0E1220), Color(0xFF1E2333))
    val onInk = cp(Color(0xFFFFFFFF), Color(0xFFF3F5FA))

    val textPrimary = cp(Color(0xFF0E1220), Color(0xFFF3F5FA))
    val textSecondary = cp(Color(0xFF7B8299), Color(0xFF9AA2B6))
    val textTertiary = cp(Color(0xFFA2A9BC), Color(0xFF6E7689))
    val textDisabled = cp(Color(0xFF9BA2B5), Color(0xFF6B7183))
    val checkboxOutline = cp(Color(0xFFD8DEEE), Color(0xFF363C4C))
    val divider = cp(Color(0xFFF1F4FC), Color(0xFF262B38))

    val success = cp(Color(0xFF15803D), Color(0xFF34D07A))
    val successSoft = cp(Color(0xFFD6F5E3), Color(0xFF1E3A2C))
    val flame = cp(Color(0xFFFFB84D), Color(0xFFFFB84D))
    val heat2 = cp(Color(0xFF7C93FF), Color(0xFF4A63C8))
    val infoSoft = cp(Color(0xFFD6EEFF), Color(0xFF2E5B77))
    val notice = cp(Color(0xFF7C3AED), Color(0xFFA78BFA))

    // Точечные значения для Bitmap-кольца (Canvas не берёт ColorProvider)
    const val WHITE = 0xFFFFFFFF.toInt()
    const val WHITE_28 = 0x47FFFFFF
}

/** Кольцо прогресса в Bitmap: полупрозрачный трек + белая дуга. Центр прозрачный. */
fun buildProgressRing(sizePx: Int, rate: Float, strokePx: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val pad = strokePx / 2f + 2f
    val rect = RectF(pad, pad, sizePx - pad, sizePx - pad)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = strokePx; strokeCap = Paint.Cap.ROUND
        color = WidgetColors.WHITE_28
    }
    canvas.drawArc(rect, 0f, 360f, false, trackPaint)

    val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = strokePx; strokeCap = Paint.Cap.ROUND
        color = WidgetColors.WHITE
    }
    val sweep = rate.coerceIn(0f, 1f) * 360f
    if (sweep > 0f) canvas.drawArc(rect, -90f, sweep, false, progressPaint)
    return bitmap
}
