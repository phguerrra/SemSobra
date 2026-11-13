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
        val extraHeight = summary.items.sumOf { if (it.item.acabouAntesDoFim) 90 else 70 }
        val height = 620 + extraHeight
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
        val margin = 35f
        val usableWidth = width - (margin * 2f)

        // Banner de Cabeçalho
        val headerHeight = 160f
        val headerPaint = Paint().apply {
            color = primaryColor
            isAntiAlias = true
        }
        val headerRect = RectF(0f, 0f, width.toFloat(), headerHeight)
        canvas.drawRect(headerRect, headerPaint)

        // Título e Subtítulos do Cabeçalho
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = if (width < 700) 26f else 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("SemSobra", margin, 60f, titlePaint)

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#D3F5E5")
            textSize = if (width < 700) 16f else 18f
            isAntiAlias = true
        }
        val dateText = "Relatório Diário • ${formatDate(summary.day.data)} (${dayName(summary.day.diaDaSemana)})"
        canvas.drawText(dateText, margin, 110f, subtitlePaint)

        val customersText = if (summary.day.clientesAtendidos > 0) "${summary.day.clientesAtendidos} clientes atendidos" else "Serviço em aberto"
        canvas.drawText(customersText, margin, 138f, subtitlePaint)

        // Métricas de Resumo
        var currentY = 195f
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
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        val cardWidth = (usableWidth - 15f) / 2f
        val cardHeight = 90f

        // Card 1: Sobra Total
        val rect1 = RectF(margin, currentY, margin + cardWidth, currentY + cardHeight)
        canvas.drawRoundRect(rect1, 16f, 16f, cardBgPaint)
        canvas.drawRoundRect(rect1, 16f, 16f, borderPaint)

        val cardLabelPaint = Paint().apply {
            color = textGray
            textSize = if (width < 700) 15f else 17f
            isAntiAlias = true
        }
        val cardValuePaint = Paint().apply {
            color = primaryColor
            textSize = if (width < 700) 20f else 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText("Sobra Total", margin + 18f, currentY + 32f, cardLabelPaint)
        canvas.drawText("${formatQuantity(totalLeftover)} (${formatWeightOneDecimal(wasteRatio)}%)", margin + 18f, currentY + 70f, cardValuePaint)

        // Card 2: Aproveitamento
        val rect2 = RectF(margin + cardWidth + 15f, currentY, margin + (cardWidth * 2f) + 15f, currentY + cardHeight)
        canvas.drawRoundRect(rect2, 16f, 16f, cardBgPaint)
        canvas.drawRoundRect(rect2, 16f, 16f, borderPaint)

        canvas.drawText("Aproveitamento", margin + cardWidth + 33f, currentY + 32f, cardLabelPaint)
        canvas.drawText("${formatWeightOneDecimal(efficiency)}%", margin + cardWidth + 33f, currentY + 70f, cardValuePaint)

        currentY += cardHeight + 45f

        // Seção da Tabela
        val sectionTitlePaint = Paint().apply {
            color = textDark
            textSize = if (width < 700) 20f else 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Detalhamento dos Pratos", margin, currentY, sectionTitlePaint)
        currentY += 24f

        // Colunas centralizadas e proporcionais
        val col1Left = margin + 18f
        val col2Center = margin + (usableWidth * 0.45f)
        val col3Center = margin + (usableWidth * 0.68f)
        val col4Center = margin + (usableWidth * 0.88f)

        // Fundo do Cabeçalho da Tabela
        val tableHeaderBgPaint = Paint().apply {
            color = Color.parseColor("#E2EAE5")
            isAntiAlias = true
        }
        val headerBgRect = RectF(margin, currentY, width - margin, currentY + 40f)
        canvas.drawRoundRect(headerBgRect, 10f, 10f, tableHeaderBgPaint)

        val headerLeftPaint = Paint().apply {
            color = secondaryColor
            textSize = if (width < 700) 15f else 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
        val headerCenterPaint = Paint().apply {
            color = secondaryColor
            textSize = if (width < 700) 15f else 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        canvas.drawText("Prato", col1Left, currentY + 26f, headerLeftPaint)
        canvas.drawText("Produzido", col2Center, currentY + 26f, headerCenterPaint)
        canvas.drawText("Sobra", col3Center, currentY + 26f, headerCenterPaint)
        canvas.drawText("Consumo", col4Center, currentY + 26f, headerCenterPaint)

        currentY += 65f

        // Linhas dos Pratos
        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E6E2")
            strokeWidth = 1.5f
        }
        val rowLeftPaint = Paint().apply {
            color = textDark
            textSize = if (width < 700) 15f else 17f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
        val rowCenterPaint = Paint().apply {
            color = textDark
            textSize = if (width < 700) 15f else 17f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val alertTextPaint = Paint().apply {
            color = Color.parseColor("#BA1A1A")
            textSize = if (width < 700) 13f else 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        val maxNameLen = if (width < 700) 14 else 20

        summary.items.forEach { display ->
            val displayName = if (display.food.nome.length > maxNameLen) {
                display.food.nome.take(maxNameLen - 1) + "…"
            } else {
                display.food.nome
            }

            canvas.drawText(displayName, col1Left, currentY, rowLeftPaint)
            canvas.drawText("${formatQuantity(display.item.quantidadeProduzida)} ${display.food.unidadeMedida}", col2Center, currentY, rowCenterPaint)
            canvas.drawText("${formatQuantity(display.item.quantidadeSobra)} ${display.food.unidadeMedida}", col3Center, currentY, rowCenterPaint)
            canvas.drawText("${formatQuantity(display.consumo)} ${display.food.unidadeMedida}", col4Center, currentY, rowCenterPaint)

            if (display.item.acabouAntesDoFim) {
                currentY += 22f
                val timeStr = display.item.horarioAcabou?.takeIf { it.isNotBlank() }?.let { " às $it" } ?: ""
                canvas.drawText("⚠️ Acabou antes do fim$timeStr", col1Left, currentY, alertTextPaint)
            }

            currentY += 20f
            canvas.drawLine(margin, currentY, width - margin, currentY, linePaint)
            currentY += 45f
        }
    }
}
