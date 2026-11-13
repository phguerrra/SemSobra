package com.project.semsobra.ui.util

import android.content.Context
import android.content.Intent
import com.project.semsobra.domain.model.QuantityPolicy
import com.project.semsobra.domain.model.ProductionSummary
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val decimalInputPattern = Regex("[0-9]*(?:[.,][0-9]*)?")

// Aceita estados intermediários de digitação; a validação do valor ocorre ao salvar.
fun isDecimalInput(value: String): Boolean = decimalInputPattern.matches(value)

fun isIntegerInput(value: String): Boolean = value.all { (it in '0'..'9') }

fun parseDoubleOrNull(value: String): Double? = value
    .trim()
    .replace(",", ".")
    .toDoubleOrNull()
    ?.takeIf(Double::isFinite)
    ?.let(QuantityPolicy::normalize)

fun filterDecimalInput(input: String): String {
    val sb = StringBuilder()
    var hasSeparator = false
    for (ch in input) {
        if (ch.isDigit()) {
            sb.append(ch)
        } else if ((ch == ',' || ch == '.') && !hasSeparator) {
            sb.append(ch)
            hasSeparator = true
        }
    }
    return sb.toString()
}

fun filterIntegerInput(input: String): String {
    return input.filter { it.isDigit() }
}

fun formatInput(value: Double): String =
    QuantityPolicy.normalize(value).let { normalized ->
        BigDecimal.valueOf(normalized).stripTrailingZeros().toPlainString().replace('.', ',')
    }

fun formatQuantity(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        maximumFractionDigits = QuantityPolicy.STORAGE_SCALE
        minimumFractionDigits = 0
    }.format(value)
}

fun formatWeightOneDecimal(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 1
    }.format(value)
}

fun formatPercentage(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }.format(value * 100.0)
}

fun formatDate(value: String): String {
    return runCatching {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }.getOrDefault(value)
}

fun dayName(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "segunda-feira"
    2 -> "terça-feira"
    3 -> "quarta-feira"
    4 -> "quinta-feira"
    5 -> "sexta-feira"
    6 -> "sábado"
    7 -> "domingo"
    else -> "-"
}

fun shareText(context: Context, text: String, title: String = "Compartilhar resumo") {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, title)
    context.startActivity(shareIntent)
}

fun buildWhatsAppSummaryText(summary: ProductionSummary): String {
    val totalProduced = summary.items.sumOf { it.item.quantidadeProduzida }
    val totalLeftover = summary.items.sumOf { it.item.quantidadeSobra }
    val totalConsumed = (totalProduced - totalLeftover).coerceAtLeast(0.0)
    val wastePercentageText = if (totalProduced > 0) {
        formatPercentage(totalLeftover / totalProduced)
    } else "0%"
    val efficiencyText = if (totalProduced > 0) {
        formatPercentage(totalConsumed / totalProduced)
    } else "100%"

    val itemsText = summary.items.joinToString("\n") { item ->
        val line = "• *${item.food.nome}:* Produzido ${formatQuantity(item.item.quantidadeProduzida)} ${item.food.unidadeMedida} | Sobra ${formatQuantity(item.item.quantidadeSobra)} ${item.food.unidadeMedida} (Consumo: ${formatQuantity(item.consumo)} ${item.food.unidadeMedida})"
        if (item.item.acabouAntesDoFim) {
            val timeText = item.item.horarioAcabou?.takeIf { it.isNotBlank() }?.let { " às $it" } ?: ""
            "$line ⚠️ *Acabou antes do fim$timeText*"
        } else {
            line
        }
    }

    val customersText = if (summary.day.clientesAtendidos > 0) "${summary.day.clientesAtendidos} pessoas" else "Em aberto"

    return """
🍃 *SemSobra — Resumo da Operação*
📅 *Data:* ${formatDate(summary.day.data)} (${dayName(summary.day.diaDaSemana)})
👥 *Clientes atendidos:* $customersText

🍳 *PRODUÇÃO E SOBRAS:*
$itemsText

📊 *BALANÇO DO DIA:*
• *Sobra Total:* ${formatQuantity(totalLeftover)} (${wastePercentageText} da produção)
• *Aproveitamento:* $efficiencyText do buffet consumido!

_Gerado pelo SemSobra App_ 🍃
    """.trimIndent()
}
