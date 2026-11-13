package com.project.semsobra.ui.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.project.semsobra.domain.model.ProductionSummary
import java.io.File
import java.io.FileOutputStream

object ReportGenerator {

    fun shareImageReport(context: Context, summary: ProductionSummary) {
        runCatching {
            val cleanData = summary.day.data.replace('/', '_').replace(':', '_').replace(' ', '_')
            val bitmap = generateReportBitmap(summary)
            val file = File(context.cacheDir, "resumo_semsobra_${cleanData}.png")
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, buildWhatsAppSummaryText(summary))
                clipData = ClipData.newRawUri("Relatório", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val resInfoList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartilhar Imagem do Resumo").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }.onFailure { error ->
            Toast.makeText(
                context,
                "Não foi possível gerar a imagem: ${error.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun sharePdfReport(context: Context, summary: ProductionSummary) {
        runCatching {
            val cleanData = summary.day.data.replace('/', '_').replace(':', '_').replace(' ', '_')
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawReportCanvas(canvas, summary, 595, 842)

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "relatorio_semsobra_${cleanData}.pdf")
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("Relatório PDF", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val resInfoList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartilhar PDF do Relatório").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }.onFailure { error ->
            Toast.makeText(
                context,
                "Não foi possível gerar o PDF: ${error.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun generateReportBitmap(summary: ProductionSummary): Bitmap {
        val width = 800
        val height = 1000 + (summary.items.size * 55)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawReportCanvas(canvas, summary, width, height)
        return bitmap
    }

    private fun drawReportCanvas(canvas: Canvas, summary: ProductionSummary, width: Int, height: Int) {
        canvas.drawColor(Color.parseColor("#F7FAF7"))

        val primaryColor = Color.parseColor("#176B4D")
        val secondaryColor = Color.parseColor("#496457")
        val textDark = Color.parseColor("#171D1A")
        val textGray = Color.parseColor("#5A645E")

        // Header Background
        val headerPaint = Paint().apply {
            color = primaryColor
            isAntiAlias = true
        }
        val headerRect = RectF(0f, 0f, width.toFloat(), 180f)
        canvas.drawRect(headerRect, headerPaint)

        // Header Text
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("SemSobra — Relatório Diário", 30f, 70f, titlePaint)

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#D3F5E5")
            textSize = 20f
            isAntiAlias = true
        }
        val dateText = "Data: ${formatDate(summary.day.data)} (${dayName(summary.day.diaDaSemana)})"
        canvas.drawText(dateText, 30f, 115f, subtitlePaint)

        val customersText = if (summary.day.clientesAtendidos > 0) "${summary.day.clientesAtendidos} clientes atendidos" else "Serviço em aberto"
        canvas.drawText(customersText, 30f, 150f, subtitlePaint)

        // Metrics Banner
        var currentY = 220f
        val totalProduced = summary.items.sumOf { it.item.quantidadeProduzida }
        val totalLeftover = summary.items.sumOf { it.item.quantidadeSobra }
        val totalConsumed = (totalProduced - totalLeftover).coerceAtLeast(0.0)
        val wasteRatio = if (totalProduced > 0) (totalLeftover / totalProduced * 100.0) else 0.0
        val efficiency = if (totalProduced > 0) (totalConsumed / totalProduced * 100.0) else 100.0

        val cardBgPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#C1C9C3")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

        val cardWidth = (width - 80f) / 2f

        // Card 1
        val rect1 = RectF(30f, currentY, 30f + cardWidth, currentY + 110f)
        canvas.drawRoundRect(rect1, 20f, 20f, cardBgPaint)
        canvas.drawRoundRect(rect1, 20f, 20f, borderPaint)

        val cardLabelPaint = Paint().apply {
            color = textGray
            textSize = 18f
            isAntiAlias = true
        }
        val cardValuePaint = Paint().apply {
            color = primaryColor
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText("Sobra Total", 50f, currentY + 40f, cardLabelPaint)
        canvas.drawText("${formatQuantity(totalLeftover)} (${formatWeightOneDecimal(wasteRatio)}%)", 50f, currentY + 85f, cardValuePaint)

        // Card 2
        val rect2 = RectF(50f + cardWidth, currentY, width - 30f, currentY + 110f)
        canvas.drawRoundRect(rect2, 20f, 20f, cardBgPaint)
        canvas.drawRoundRect(rect2, 20f, 20f, borderPaint)

        canvas.drawText("Aproveitamento", 70f + cardWidth, currentY + 40f, cardLabelPaint)
        canvas.drawText("${formatWeightOneDecimal(efficiency)}%", 70f + cardWidth, currentY + 85f, cardValuePaint)

        currentY += 150f

        // Table Header
        val sectionTitlePaint = Paint().apply {
            color = textDark
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Detalhamento de Pratos", 30f, currentY, sectionTitlePaint)
        currentY += 20f

        val linePaint = Paint().apply {
            color = Color.parseColor("#C1C9C3")
            strokeWidth = 2f
        }
        canvas.drawLine(30f, currentY, width - 30f, currentY, linePaint)
        currentY += 35f

        val tableHeaderPaint = Paint().apply {
            color = secondaryColor
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Prato", 40f, currentY, tableHeaderPaint)
        canvas.drawText("Produzido", 360f, currentY, tableHeaderPaint)
        canvas.drawText("Sobra", 520f, currentY, tableHeaderPaint)
        canvas.drawText("Consumo", 660f, currentY, tableHeaderPaint)

        currentY += 15f
        canvas.drawLine(30f, currentY, width - 30f, currentY, linePaint)
        currentY += 35f

        // Table Rows
        val rowTextPaint = Paint().apply {
            color = textDark
            textSize = 19f
            isAntiAlias = true
        }
        val alertTextPaint = Paint().apply {
            color = Color.parseColor("#BA1A1A")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        summary.items.forEach { display ->
            canvas.drawText(display.food.nome.take(22), 40f, currentY, rowTextPaint)
            canvas.drawText("${formatQuantity(display.item.quantidadeProduzida)} ${display.food.unidadeMedida}", 360f, currentY, rowTextPaint)
            canvas.drawText("${formatQuantity(display.item.quantidadeSobra)} ${display.food.unidadeMedida}", 520f, currentY, rowTextPaint)
            canvas.drawText("${formatQuantity(display.consumo)} ${display.food.unidadeMedida}", 660f, currentY, rowTextPaint)

            if (display.item.acabouAntesDoFim) {
                currentY += 22f
                val timeStr = display.item.horarioAcabou?.takeIf { it.isNotBlank() }?.let { " às $it" } ?: ""
                canvas.drawText("⚠️ Acabou antes do fim$timeStr", 40f, currentY, alertTextPaint)
            }

            currentY += 35f
            canvas.drawLine(30f, currentY - 15f, width - 30f, currentY - 15f, linePaint)
        }

        // Footer
        val footerPaint = Paint().apply {
            color = textGray
            textSize = 16f
            isAntiAlias = true
        }
        canvas.drawText("SemSobra — Gestão Inteligente e Combate ao Desperdício 🍃", 30f, height - 30f, footerPaint)
    }
}
