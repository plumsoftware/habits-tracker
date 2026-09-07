package ru.plumsoftware.habitstracker.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.plumsoftware.habitstracker.data.database.CategoryRepository
import ru.plumsoftware.habitstracker.data.database.HabitRepository
import ru.plumsoftware.habitstracker.data.model.Periodicity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportManager(
    private val context: Context,
    private val habitRepository: HabitRepository,
    private val categoryRepository: CategoryRepository
) {
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private data class Row(
        val title: String,
        val categoryName: String,
        val completedCount: Int,
        val periodicity: Periodicity
    )

    private data class ExportData(
        val from: Long,
        val to: Long,
        val rows: List<Row>
    ) {
        val totalCompletions: Int get() = rows.sumOf { it.completedCount }
    }

    /** Собирает данные по всем привычкам за диапазон [from]..[to] (включительно). */
    private suspend fun collect(from: Long, to: Long): ExportData {
        val habits = habitRepository.getAllHabits().first()
        val categories = categoryRepository.getAll().first().associateBy { it.id }

        val rows = habits.map { habit ->
            val completions = habitRepository.habitCompletionDao
                .getCompletionsInPeriod(habit.id, from, to)
            Row(
                title = habit.title.trim(),
                categoryName = habit.categoryId?.let { categories[it]?.name } ?: "—",
                completedCount = completions.count { it.completed },
                periodicity = habit.periodicity
            )
        }
        return ExportData(from, to, rows)
    }

    private fun exportsDir(): File = File(context.cacheDir, "exports").apply { mkdirs() }

    private fun periodicityLabel(p: Periodicity): String = when (p) {
        Periodicity.DAILY -> "Каждый день"
        Periodicity.WEEKLY -> "Каждую неделю"
    }

    /** Генерирует CSV-файл. Разделитель «;», UTF-8 с BOM для корректного открытия в Excel. */
    suspend fun exportCsv(from: Long, to: Long): File = withContext(Dispatchers.IO) {
        val data = collect(from, to)
        val file = File(exportsDir(), "habits_${System.currentTimeMillis()}.csv")

        fun escape(value: String): String {
            return if (value.contains(';') || value.contains('"') || value.contains('\n')) {
                "\"" + value.replace("\"", "\"\"") + "\""
            } else value
        }

        val sb = StringBuilder()
        sb.append('﻿') // BOM
        sb.append("Трекер привычек — экспорт статистики\n")
        sb.append("Период;${dateFormat.format(Date(data.from))} - ${dateFormat.format(Date(data.to))}\n")
        sb.append("\n")
        sb.append("Привычка;Категория;Выполнено (раз);Периодичность\n")
        data.rows.forEach { row ->
            sb.append(
                listOf(
                    escape(row.title),
                    escape(row.categoryName),
                    row.completedCount.toString(),
                    escape(periodicityLabel(row.periodicity))
                ).joinToString(";")
            )
            sb.append("\n")
        }
        sb.append("\n")
        sb.append("Итого выполнений;${data.totalCompletions}\n")

        file.writeText(sb.toString(), Charsets.UTF_8)
        file
    }

    /** Генерирует PDF-файл со сводкой (A4, с простой пагинацией). */
    suspend fun exportPdf(from: Long, to: Long): File = withContext(Dispatchers.IO) {
        val data = collect(from, to)
        val file = File(exportsDir(), "habits_${System.currentTimeMillis()}.pdf")

        val pageWidth = 595
        val pageHeight = 842
        val marginLeft = 40f
        val marginTop = 50f
        val marginBottom = 50f

        val document = PdfDocument()

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 20f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            color = 0xFF5B6478.toInt()
        }
        val headerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
        }

        // Колонки: Привычка | Категория | Выполнено
        val colTitleX = marginLeft
        val colCategoryX = marginLeft + 260f
        val colCountX = marginLeft + 420f

        var pageNumber = 1
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas = page.canvas
        var y = marginTop

        // Шапка
        canvas.drawText("Трекер привычек", marginLeft, y, titlePaint)
        y += 24f
        canvas.drawText(
            "Период: ${dateFormat.format(Date(data.from))} - ${dateFormat.format(Date(data.to))}",
            marginLeft, y, subtitlePaint
        )
        y += 30f

        // Заголовок таблицы
        canvas.drawText("Привычка", colTitleX, y, headerPaint)
        canvas.drawText("Категория", colCategoryX, y, headerPaint)
        canvas.drawText("Выполнено", colCountX, y, headerPaint)
        y += 6f
        canvas.drawLine(marginLeft, y, pageWidth - marginLeft, y, bodyPaint)
        y += 18f

        fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
            if (paint.measureText(text) <= maxWidth) return text
            var result = text
            while (result.isNotEmpty() && paint.measureText("$result…") > maxWidth) {
                result = result.dropLast(1)
            }
            return "$result…"
        }

        data.rows.forEach { row ->
            if (y > pageHeight - marginBottom) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = page.canvas
                y = marginTop
            }
            canvas.drawText(ellipsize(row.title, bodyPaint, 250f), colTitleX, y, bodyPaint)
            canvas.drawText(ellipsize(row.categoryName, bodyPaint, 150f), colCategoryX, y, bodyPaint)
            canvas.drawText(row.completedCount.toString(), colCountX, y, bodyPaint)
            y += 20f
        }

        y += 10f
        if (y > pageHeight - marginBottom) {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            canvas = page.canvas
            y = marginTop
        }
        canvas.drawText("Итого выполнений: ${data.totalCompletions}", marginLeft, y, headerPaint)

        document.finishPage(page)
        file.outputStream().use { document.writeTo(it) }
        document.close()
        file
    }

    private fun uriFor(file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    /** Открывает системный диалог «Поделиться» для файла. */
    fun share(file: File, mime: String) {
        val uri = uriFor(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Поделиться").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    companion object {
        val exportModule = module {
            single<ExportManager> {
                ExportManager(
                    context = androidContext(),
                    habitRepository = get(),
                    categoryRepository = get()
                )
            }
        }
    }
}
